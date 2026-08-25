#!/usr/bin/env python3
"""Evaluate the exported LML local intent model on MASSIVE fr-FR test rows."""

from __future__ import annotations

import argparse
import importlib.util
import json
from collections import defaultdict
from pathlib import Path

import numpy as np


def load_training_module(path: Path):
    spec = importlib.util.spec_from_file_location("train_intent_model", path)
    if spec is None or spec.loader is None:
        raise RuntimeError("Unable to load training module")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--dataset", required=True, type=Path)
    parser.add_argument("--model", required=True, type=Path)
    parser.add_argument("--trainer", default=Path(__file__).with_name("train_intent_model.py"), type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()

    trainer = load_training_module(args.trainer)
    model = json.loads(args.model.read_text(encoding="utf-8"))
    labels = model["labels"]
    label_to_id = {label: index for index, label in enumerate(labels)}
    weights = model["weights"]
    w1 = np.array(weights["w1"], dtype=np.float32)
    b1 = np.array(weights["b1"], dtype=np.float32)
    w2 = np.array(weights["w2"], dtype=np.float32)
    b2 = np.array(weights["b2"], dtype=np.float32)
    threshold = float(model["threshold"])

    rows = []
    with args.dataset.open("r", encoding="utf-8") as source:
        for line in source:
            record = json.loads(line)
            if record["partition"] == "test":
                rows.append((trainer.action_label(record["intent"]), record["utt"]))

    x = np.stack([trainer.vectorize(text) for _, text in rows])
    expected = np.array([label_to_id[label] for label, _ in rows], dtype=np.int64)
    hidden = np.maximum(0.0, x @ w1 + b1)
    logits = hidden @ w2 + b2
    probabilities = trainer.softmax(logits)
    predicted = probabilities.argmax(axis=1)
    confidences = probabilities.max(axis=1)
    predicted = np.where(confidences >= threshold, predicted, label_to_id["NONE"])

    labels_metrics = {}
    for label, label_id in label_to_id.items():
        true_positive = int(((predicted == label_id) & (expected == label_id)).sum())
        false_positive = int(((predicted == label_id) & (expected != label_id)).sum())
        false_negative = int(((predicted != label_id) & (expected == label_id)).sum())
        precision = true_positive / (true_positive + false_positive) if true_positive + false_positive else 0.0
        recall = true_positive / (true_positive + false_negative) if true_positive + false_negative else 0.0
        labels_metrics[label] = {
            "precision": round(precision, 4),
            "recall": round(recall, 4),
            "support": int((expected == label_id).sum()),
        }

    balanced_accuracy = sum(item["recall"] for item in labels_metrics.values()) / len(labels_metrics)
    report = {
        "test_examples": len(rows),
        "threshold": threshold,
        "accuracy": round(float((predicted == expected).mean()), 4),
        "balanced_accuracy": round(balanced_accuracy, 4),
        "per_label": labels_metrics,
        "allowlist_verified": sorted(labels) == ["CALENDAR", "EMAIL", "MAPS", "NONE", "WEB"],
    }
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
