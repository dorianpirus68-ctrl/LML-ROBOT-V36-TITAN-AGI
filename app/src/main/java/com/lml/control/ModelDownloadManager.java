package com.lml.control;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public final class ModelDownloadManager {
    public static final String MODEL_NAME = "Qwen3 0.6B INT4";
    public static final String MODEL_URL = "https://huggingface.co/litert-community/Qwen3-0.6B/resolve/main/Qwen3-0.6B_dynamic_wi4b32_afp32.litertlm?download=true";
    private static final String MODEL_FILE = "qwen3_0_6b_int4.litertlm";

    public interface Listener {
        void onProgress(int percent);
        void onComplete(File modelFile);
        void onError(String message);
    }

    private ModelDownloadManager() { }

    public static File getModelFile(Context context) {
        File directory = new File(context.getFilesDir(), "models");
        if (!directory.exists()) directory.mkdirs();
        return new File(directory, MODEL_FILE);
    }

    public static boolean isInstalled(Context context) {
        File model = getModelFile(context);
        return model.isFile() && model.length() > 1024L * 1024L;
    }

    public static void download(Context context, Listener listener) {
        new Thread(() -> {
            File output = getModelFile(context);
            File temporary = new File(output.getParentFile(), output.getName() + ".part");
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(MODEL_URL).openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(30000);
                connection.setRequestProperty("Accept", "application/octet-stream");
                connection.connect();
                int response = connection.getResponseCode();
                if (response < 200 || response >= 300) throw new IllegalStateException("Téléchargement refusé (HTTP " + response + ")");
                long total = connection.getContentLengthLong();
                try (InputStream input = new BufferedInputStream(connection.getInputStream());
                     FileOutputStream stream = new FileOutputStream(temporary)) {
                    byte[] buffer = new byte[1024 * 32];
                    long downloaded = 0L;
                    int lastPercent = -1;
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        stream.write(buffer, 0, count);
                        downloaded += count;
                        if (total > 0) {
                            int percent = (int) ((downloaded * 100L) / total);
                            if (percent != lastPercent) {
                                lastPercent = percent;
                                listener.onProgress(percent);
                            }
                        }
                    }
                }
                if (output.exists()) output.delete();
                if (!temporary.renameTo(output)) throw new IllegalStateException("Impossible de finaliser le modèle");
                listener.onComplete(output);
            } catch (Exception error) {
                if (temporary.exists()) temporary.delete();
                listener.onError(error.getMessage() == null ? "Échec de téléchargement" : error.getMessage());
            } finally {
                if (connection != null) connection.disconnect();
            }
        }, "lml-model-download").start();
    }
}
