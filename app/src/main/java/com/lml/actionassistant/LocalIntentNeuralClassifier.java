package com.lml.actionassistant;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;

/**
 * A compact, on-device MLP for intent suggestions. It is intentionally separate
 * from Android action execution: a prediction can only select an item from the
 * application's fixed allowlist, and MainActivity still presents an HITL preview.
 */
public final class LocalIntentNeuralClassifier {
    public enum Action {
        NONE, WEB, MAPS, EMAIL, CALENDAR
    }

    public static final class Prediction {
        private final Action action;
        private final float confidence;
        private final String label;

        private Prediction(Action action, float confidence, String label) {
            this.action = action;
            this.confidence = confidence;
            this.label = label;
        }

        public Action getAction() {
            return action;
        }

        public float getConfidence() {
            return confidence;
        }

        public String getLabel() {
            return label;
        }
    }

    private final int featureCount;
    private final int hiddenCount;
    private final float threshold;
    private final String[] labels;
    private final float[][] w1;
    private final float[] b1;
    private final float[][] w2;
    private final float[] b2;

    private LocalIntentNeuralClassifier(JSONObject model) throws Exception {
        featureCount = model.getInt("feature_count");
        hiddenCount = model.getInt("hidden_count");
        threshold = (float) model.getDouble("threshold");
        labels = strings(model.getJSONArray("labels"));
        JSONObject weights = model.getJSONObject("weights");
        w1 = matrix(weights.getJSONArray("w1"));
        b1 = vector(weights.getJSONArray("b1"));
        w2 = matrix(weights.getJSONArray("w2"));
        b2 = vector(weights.getJSONArray("b2"));

        if (w1.length != featureCount || w1[0].length != hiddenCount
                || w2.length != hiddenCount || w2[0].length != labels.length
                || b1.length != hiddenCount || b2.length != labels.length) {
            throw new IllegalStateException("Dimensions du modèle neuronal incompatibles.");
        }
    }

    public static LocalIntentNeuralClassifier load(Context context) throws Exception {
        try (InputStream stream = context.getAssets().open("lml_intent_model.json");
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = stream.read(buffer)) != -1) output.write(buffer, 0, count);
            JSONObject model = new JSONObject(output.toString(StandardCharsets.UTF_8.name()));
            return new LocalIntentNeuralClassifier(model);
        }
    }

    public Prediction predict(String text) {
        float[] input = vectorize(text);
        float[] hidden = new float[hiddenCount];
        for (int column = 0; column < hiddenCount; column++) {
            float total = b1[column];
            for (int row = 0; row < featureCount; row++) total += input[row] * w1[row][column];
            hidden[column] = Math.max(0f, total);
        }

        float[] logits = new float[labels.length];
        float maximum = -Float.MAX_VALUE;
        for (int column = 0; column < labels.length; column++) {
            float total = b2[column];
            for (int row = 0; row < hiddenCount; row++) total += hidden[row] * w2[row][column];
            logits[column] = total;
            maximum = Math.max(maximum, total);
        }

        float normalizer = 0f;
        for (int index = 0; index < logits.length; index++) {
            logits[index] = (float) Math.exp(logits[index] - maximum);
            normalizer += logits[index];
        }
        int bestIndex = 0;
        float best = 0f;
        for (int index = 0; index < logits.length; index++) {
            float probability = logits[index] / normalizer;
            if (probability > best) {
                best = probability;
                bestIndex = index;
            }
        }

        String label = labels[bestIndex];
        Action action = parseAction(label);
        if (best < threshold) action = Action.NONE;
        return new Prediction(action, best, label);
    }

    private float[] vectorize(String text) {
        float[] output = new float[featureCount];
        String normalised = Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        for (String token : normalised.split("[^a-z0-9]+")) {
            if (token.length() <= 1) continue;
            output[index("w:" + token)] = 1f;
            String padded = "<" + token + ">";
            for (int width : new int[]{3, 4}) {
                for (int offset = 0; offset <= padded.length() - width; offset++) {
                    output[index("c:" + padded.substring(offset, offset + width))] = 1f;
                }
            }
        }
        return output;
    }

    private int index(String value) {
        int hash = 0x811C9DC5;
        for (byte item : value.getBytes(StandardCharsets.UTF_8)) {
            hash ^= item & 0xFF;
            hash *= 0x01000193;
        }
        return (hash & 0x7FFFFFFF) % featureCount;
    }

    private static Action parseAction(String label) {
        try {
            return Action.valueOf(label);
        } catch (IllegalArgumentException exception) {
            return Action.NONE;
        }
    }

    private static String[] strings(JSONArray values) throws Exception {
        String[] output = new String[values.length()];
        for (int index = 0; index < values.length(); index++) output[index] = values.getString(index);
        return output;
    }

    private static float[] vector(JSONArray values) throws Exception {
        float[] output = new float[values.length()];
        for (int index = 0; index < values.length(); index++) output[index] = (float) values.getDouble(index);
        return output;
    }

    private static float[][] matrix(JSONArray values) throws Exception {
        float[][] output = new float[values.length()][];
        for (int index = 0; index < values.length(); index++) output[index] = vector(values.getJSONArray(index));
        return output;
    }
}
