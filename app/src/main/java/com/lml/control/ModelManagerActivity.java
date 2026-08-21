package com.lml.control;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class ModelManagerActivity extends Activity {
    private TextView status;
    private ProgressBar progress;
    private Button download;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);
        root.setBackgroundColor(Color.rgb(6, 15, 29));

        TextView title = label("MOTEUR LOCAL", 26, Color.rgb(91, 245, 255));
        root.addView(title);
        TextView detail = label("Qwen3 0.6B INT4 est téléchargé dans le stockage privé de l’application. Le modèle ne se trouve pas dans l’APK : le téléchargement demande une connexion et plusieurs centaines de Mo d’espace libre.", 16, Color.WHITE);
        detail.setPadding(0, 16, 0, 18);
        root.addView(detail);

        status = label("", 17, Color.rgb(205, 245, 255));
        root.addView(status);
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        root.addView(progress, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        download = new Button(this);
        download.setText("Télécharger le moteur local");
        download.setOnClickListener(v -> startDownload());
        root.addView(download, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView guard = label("GARDE-FOU : le modèle propose du texte et un plan. L’application, et non le modèle, contrôle les actions autorisées, les validations et l’expiration des tickets.", 15, Color.rgb(255, 205, 90));
        guard.setPadding(0, 22, 0, 0);
        root.addView(guard);
        setContentView(root);
        refresh();
    }

    private TextView label(String text, int size, int color) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.START);
        return view;
    }

    private void refresh() {
        File file = ModelDownloadManager.getModelFile(this);
        boolean ready = ModelDownloadManager.isInstalled(this);
        status.setText(ready ? "ÉTAT : moteur installé — " + (file.length() / (1024 * 1024)) + " Mo" : "ÉTAT : moteur non installé");
        download.setEnabled(!ready);
        if (ready) download.setText("Moteur local prêt");
    }

    private void startDownload() {
        download.setEnabled(false);
        status.setText("TÉLÉCHARGEMENT EN COURS…");
        ModelDownloadManager.download(this, new ModelDownloadManager.Listener() {
            @Override public void onProgress(int percent) { runOnUiThread(() -> { progress.setProgress(percent); status.setText("TÉLÉCHARGEMENT : " + percent + "%"); }); }
            @Override public void onComplete(File modelFile) { runOnUiThread(() -> { Toast.makeText(ModelManagerActivity.this, "Moteur local installé", Toast.LENGTH_LONG).show(); refresh(); }); }
            @Override public void onError(String message) { runOnUiThread(() -> { download.setEnabled(true); status.setText("ÉCHEC : " + message); }); }
        });
    }
}
