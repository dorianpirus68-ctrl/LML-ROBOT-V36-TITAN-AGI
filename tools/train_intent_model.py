#!/usr/bin/env python3
"""Train a compact French intent neural classifier for LML Action Assistant.

The model maps only a restricted, locally defined allowlist of intent classes to
suggestions. It never executes Android actions and never learns app-control intents.
Source data: MASSIVE fr-FR (CC BY 4.0), downloaded separately from Amazon's
published archive. See NOTICE-MODEL.md for attribution.
"""

from __future__ import annotations

import argparse
import json
import math
import re
import unicodedata
from collections import Counter, defaultdict
from pathlib import Path

import numpy as np

FEATURE_COUNT = 768
HIDDEN_COUNT = 48
LABELS = ["NONE", "WEB", "MAPS", "EMAIL", "CALENDAR"]

WEB_INTENTS = {
    "weather_query", "news_query", "qa_currency", "qa_definition", "qa_factoid",
    "qa_maths", "qa_stock", "cooking_query", "cooking_recipe",
    "recommendation_events", "recommendation_movies",
}
MAPS_INTENTS = {
    "transport_query", "transport_taxi", "transport_ticket", "transport_traffic",
    "recommendation_locations",
}


def action_label(intent: str) -> str:
    if intent in WEB_INTENTS:
        return "WEB"
    if intent in MAPS_INTENTS:
        return "MAPS"
    if intent == "email_sendemail":
        return "EMAIL"
    if intent == "calendar_set":
        return "CALENDAR"
    return "NONE"


def normalize(text: str) -> list[str]:
    text = unicodedata.normalize("NFD", text.lower())
    text = "".join(char for char in text if unicodedata.category(char) != "Mn")
    return [token for token in re.split(r"[^a-z0-9]+", text) if len(token) > 1]


def fnv1a(token: str) -> int:
    value = 2166136261
    for byte in token.encode("utf-8"):
        value ^= byte
        value = (value * 16777619) & 0xFFFFFFFF
    return value


def vectorize(text: str) -> np.ndarray:
    vector = np.zeros(FEATURE_COUNT, dtype=np.float32)
    tokens = normalize(text)
    for token in tokens:
        vector[fnv1a("w:" + token) % FEATURE_COUNT] = 1.0
        padded = "<" + token + ">"
        for width in (3, 4):
            for offset in range(len(padded) - width + 1):
                vector[fnv1a("c:" + padded[offset:offset + width]) % FEATURE_COUNT] = 1.0
    return vector


def read_examples(path: Path) -> list[tuple[str, str, str]]:
    examples: list[tuple[str, str, str]] = []
    with path.open("r", encoding="utf-8") as source:
        for line in source:
            record = json.loads(line)
            examples.append((record["partition"], action_label(record["intent"]), record["utt"]))
    return examples


def weighted_train_examples(examples: list[tuple[str, str, str]], seed: int) -> tuple[list[tuple[str, str]], dict[str, int]]:
    grouped: dict[str, list[str]] = defaultdict(list)
    for partition, label, utterance in examples:
        if partition == "train":
            grouped[label].append(utterance)

    if set(grouped) != set(LABELS):
        missing = sorted(set(LABELS) - set(grouped))
        raise RuntimeError(f"Missing expected labels: {missing}")

    selected = [(label, utterance) for label in LABELS for utterance in grouped[label]]
    np.random.default_rng(seed).shuffle(selected)
    return selected, {label: len(grouped[label]) for label in LABELS}


def softmax(values: np.ndarray) -> np.ndarray:
    shifted = values - values.max(axis=1, keepdims=True)
    exponent = np.exp(shifted)
    return exponent / exponent.sum(axis=1, keepdims=True)


def evaluate(x: np.ndarray, y: np.ndarray, w1: np.ndarray, b1: np.ndarray,
             w2: np.ndarray, b2: np.ndarray) -> tuple[float, float]:
    hidden = np.maximum(0.0, x @ w1 + b1)
    probabilities = softmax(hidden @ w2 + b2)
    predicted = probabilities.argmax(axis=1)
    accuracy = float((predicted == y).mean())
    recalls = []
    for label_id in range(len(LABELS)):
        subset = y == label_id
        recalls.append(float((predicted[subset] == label_id).mean()) if subset.any() else 0.0)
    return accuracy, float(sum(recalls) / len(recalls))


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--epochs", type=int, default=340)
    parser.add_argument("--learning-rate", type=float, default=0.035)
    arguments = parser.parse_args()

    all_examples = read_examples(arguments.input)
    training, class_counts = weighted_train_examples(all_examples, arguments.seed)
    label_to_id = {label: index for index, label in enumerate(LABELS)}
    x = np.stack([vectorize(utterance) for _, utterance in training])
    y = np.array([label_to_id[label] for label, _ in training], dtype=np.int64)
    one_hot = np.eye(len(LABELS), dtype=np.float32)[y]
    sample_weights = np.array([1.0 / class_counts[label] for label, _ in training], dtype=np.float32)

    rng = np.random.default_rng(arguments.seed)
    w1 = rng.normal(0.0, math.sqrt(2.0 / FEATURE_COUNT), (FEATURE_COUNT, HIDDEN_COUNT)).astype(np.float32)
    b1 = np.zeros(HIDDEN_COUNT, dtype=np.float32)
    w2 = rng.normal(0.0, math.sqrt(2.0 / HIDDEN_COUNT), (HIDDEN_COUNT, len(LABELS))).astype(np.float32)
    b2 = np.zeros(len(LABELS), dtype=np.float32)

    for _ in range(arguments.epochs):
        hidden_linear = x @ w1 + b1
        hidden = np.maximum(0.0, hidden_linear)
        probabilities = softmax(hidden @ w2 + b2)
        output_gradient = (probabilities - one_hot) * sample_weights[:, None] / sample_weights.sum()
        grad_w2 = hidden.T @ output_gradient
        grad_b2 = output_gradient.sum(axis=0)
        hidden_gradient = (output_gradient @ w2.T) * (hidden_linear > 0.0)
        grad_w1 = x.T @ hidden_gradient
        grad_b1 = hidden_gradient.sum(axis=0)
        w2 -= arguments.learning_rate * grad_w2
        b2 -= arguments.learning_rate * grad_b2
        w1 -= arguments.learning_rate * grad_w1
        b1 -= arguments.learning_rate * grad_b1

    test_rows = [(label, utterance) for partition, label, utterance in all_examples if partition == "test"]
    x_test = np.stack([vectorize(utterance) for _, utterance in test_rows])
    y_test = np.array([label_to_id[label] for label, _ in test_rows], dtype=np.int64)
    accuracy, balanced_accuracy = evaluate(x_test, y_test, w1, b1, w2, b2)

    output = {
        "format_version": 1,
        "model_type": "hashed_bow_mlp_relu_softmax",
        "feature_count": FEATURE_COUNT,
        "hidden_count": HIDDEN_COUNT,
        "labels": LABELS,
        "threshold": 0.62,
        "normalization": "NFD lowercase; strip combining marks; split on non-alphanumeric; tokens length > 1",
        "hash": "FNV-1a UTF-8 modulo feature_count; word tokens and character 3-4 grams",
        "training": {
            "dataset": "MASSIVE 1.0 fr-FR",
            "license": "CC BY 4.0",
            "source": "https://amazon-massive-nlu-dataset.s3.amazonaws.com/amazon-massive-dataset-1.0.tar.gz",
            "seed": arguments.seed,
            "epochs": arguments.epochs,
            "learning_rate": arguments.learning_rate,
            "weighted_train_examples": len(training),
            "test_examples": len(test_rows),
            "test_accuracy": round(accuracy, 4),
            "test_balanced_accuracy": round(balanced_accuracy, 4),
            "class_distribution": class_counts,
        },
        "weights": {
            "w1": w1.tolist(), "b1": b1.tolist(), "w2": w2.tolist(), "b2": b2.tolist(),
        },
    }
    arguments.output.parent.mkdir(parents=True, exist_ok=True)
    with arguments.output.open("w", encoding="utf-8") as destination:
        json.dump(output, destination, ensure_ascii=False, separators=(",", ":"))

    print(json.dumps(output["training"], ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
