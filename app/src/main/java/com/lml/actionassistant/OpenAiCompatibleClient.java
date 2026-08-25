package com.lml.actionassistant;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/** A minimal client for OpenAI-compatible chat-completions servers, including local /v1 endpoints. */
public final class OpenAiCompatibleClient {
    private static final int CONNECT_TIMEOUT_MS = 20_000;
    private static final int READ_TIMEOUT_MS = 90_000;

    public String review(String baseUrl, String model, String apiKey, String systemInstruction, String userTask)
            throws Exception {
        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("temperature", 0.2);
        body.put("stream", false);

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system").put("content", systemInstruction));
        messages.put(new JSONObject().put("role", "user").put("content", userTask));
        body.put("messages", messages);

        JSONObject response = postJson(chatCompletionsUrl(baseUrl), body, apiKey);
        JSONArray choices = response.optJSONArray("choices");
        if (choices == null || choices.length() == 0) {
            throw new IllegalStateException("La réponse du serveur ne contient aucune proposition.");
        }
        JSONObject firstChoice = choices.getJSONObject(0);
        JSONObject message = firstChoice.optJSONObject("message");
        String content = message == null ? "" : message.optString("content", "").trim();
        if (content.isEmpty()) {
            throw new IllegalStateException("La réponse du serveur est vide.");
        }
        return content;
    }

    public void test(String baseUrl, String model, String apiKey, String systemInstruction) throws Exception {
        String result = review(baseUrl, model, apiKey, systemInstruction,
                "Répondez exactement par le mot OK pour confirmer que la connexion fonctionne.");
        if (result.isEmpty()) {
            throw new IllegalStateException("Le serveur a renvoyé une réponse vide.");
        }
    }

    public static boolean isValidBaseUrl(String rawUrl) {
        try {
            URI uri = URI.create(rawUrl == null ? "" : rawUrl.trim());
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && !uri.getHost().isEmpty();
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private JSONObject postJson(String url, JSONObject payload, String apiKey) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setRequestProperty("Accept", "application/json");
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
        }

        byte[] bytes = payload.toString().getBytes(StandardCharsets.UTF_8);
        connection.setFixedLengthStreamingMode(bytes.length);
        try (OutputStream output = connection.getOutputStream()) {
            output.write(bytes);
        }

        int statusCode = connection.getResponseCode();
        InputStream stream = statusCode >= 200 && statusCode < 300
                ? connection.getInputStream() : connection.getErrorStream();
        String text = readFully(stream);
        if (statusCode < 200 || statusCode >= 300) {
            String detail = text.length() > 500 ? text.substring(0, 500) + "…" : text;
            throw new IllegalStateException("Le serveur a renvoyé HTTP " + statusCode
                    + (detail.isEmpty() ? "." : " : " + detail));
        }
        return new JSONObject(text);
    }

    private String chatCompletionsUrl(String baseUrl) {
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized + "/chat/completions";
    }

    private String readFully(InputStream stream) throws Exception {
        if (stream == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }
}
