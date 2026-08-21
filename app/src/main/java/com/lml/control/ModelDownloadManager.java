package com.lml.control;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** Downloads a model into app-private storage. Integrity is checked by the runtime before activation. */
public final class ModelDownloadManager {
    public static final String MODEL_NAME = "Qwen3 0.6B INT4";
    public static final String MODEL_URL = "https://huggingface.co/litert-community/Qwen3-0.6B/resolve/main/Qwen3-0.6B_dynamic_wi4b32_afp32.litertlm?download=true";
    private static final String MODEL_FILE = "qwen3_0_6b_int4.litertlm";
    private static final long MIN_FREE_SPACE = 600L * 1024L * 1024L;
    private static final long MAX_DOWNLOAD_SIZE = 2_000L * 1024L * 1024L;

    public interface Listener { void onProgress(int percent); void onComplete(File modelFile); void onError(String message); }
    private ModelDownloadManager() { }

    public static File getModelFile(Context context) {
        File directory = new File(context.getFilesDir(), "models");
        if (!directory.exists()) directory.mkdirs();
        return new File(directory, MODEL_FILE);
    }
    public static boolean isInstalled(Context context) { File model = getModelFile(context); return model.isFile() && model.length() > 1024L * 1024L; }

    public static void download(Context context, Listener listener) {
        new Thread(() -> {
            File output = getModelFile(context);
            File temporary = new File(output.getParentFile(), output.getName() + ".part");
            HttpURLConnection connection = null;
            try {
                if (output.getParentFile().getUsableSpace() < MIN_FREE_SPACE) throw new IllegalStateException("Espace libre insuffisant pour le modèle local");
                URL url = new URL(MODEL_URL);
                if (!"https".equalsIgnoreCase(url.getProtocol()) || !"huggingface.co".equalsIgnoreCase(url.getHost())) throw new IllegalStateException("Source de modèle non autorisée");
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(15_000); connection.setReadTimeout(30_000); connection.setRequestProperty("Accept", "application/octet-stream"); connection.connect();
                int response = connection.getResponseCode();
                if (response < 200 || response >= 300) throw new IllegalStateException("Téléchargement refusé (HTTP " + response + ")");
                long total = connection.getContentLengthLong();
                if (total > MAX_DOWNLOAD_SIZE) throw new IllegalStateException("Modèle trop volumineux pour ce profil local");
                try (InputStream input = new BufferedInputStream(connection.getInputStream()); FileOutputStream stream = new FileOutputStream(temporary)) {
                    byte[] buffer = new byte[32 * 1024]; long downloaded = 0L; int lastPercent = -1; int count;
                    while ((count = input.read(buffer)) != -1) {
                        stream.write(buffer, 0, count); downloaded += count;
                        if (downloaded > MAX_DOWNLOAD_SIZE) throw new IllegalStateException("Téléchargement interrompu : taille inattendue");
                        if (total > 0) { int percent = (int) ((downloaded * 100L) / total); if (percent != lastPercent) { lastPercent = percent; listener.onProgress(percent); } }
                    }
                    stream.getFD().sync();
                }
                if (temporary.length() < 1024L * 1024L) throw new IllegalStateException("Modèle incomplet");
                if (output.exists() && !output.delete()) throw new IllegalStateException("Impossible de remplacer le modèle précédent");
                if (!temporary.renameTo(output)) throw new IllegalStateException("Impossible de finaliser le modèle");
                NexusAuditLog.record(context, "model_downloaded", MODEL_NAME); listener.onComplete(output);
            } catch (Exception error) {
                if (temporary.exists()) temporary.delete(); NexusAuditLog.record(context, "model_download_failed", error.getClass().getSimpleName()); listener.onError(error.getMessage() == null ? "Échec de téléchargement" : error.getMessage());
            } finally { if (connection != null) connection.disconnect(); }
        }, "lml-model-download").start();
    }
}
