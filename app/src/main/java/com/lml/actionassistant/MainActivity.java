package com.lml.actionassistant;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends AppCompatActivity {
    private TextInputEditText baseUrlInput;
    private TextInputEditText modelInput;
    private TextInputEditText apiKeyInput;
    private TextInputEditText taskInput;
    private TextView responseText;
    private TextView statusText;
    private MaterialButton saveButton;
    private MaterialButton testConnectionButton;
    private MaterialButton reviewButton;

    private SecureConfigStore configStore;
    private final OpenAiCompatibleClient aiClient = new OpenAiCompatibleClient();
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private volatile boolean busy = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        configStore = new SecureConfigStore(getApplicationContext());
        baseUrlInput = findViewById(R.id.baseUrlInput);
        modelInput = findViewById(R.id.modelInput);
        apiKeyInput = findViewById(R.id.apiKeyInput);
        taskInput = findViewById(R.id.taskInput);
        responseText = findViewById(R.id.responseText);
        statusText = findViewById(R.id.statusText);
        saveButton = findViewById(R.id.saveButton);
        testConnectionButton = findViewById(R.id.testConnectionButton);
        reviewButton = findViewById(R.id.reviewButton);

        baseUrlInput.setText(configStore.getBaseUrl());
        modelInput.setText(configStore.getModel());
        if (configStore.hasApiKey()) {
            apiKeyInput.setHint("Clé chiffrée déjà enregistrée — saisissez une nouvelle valeur pour la remplacer");
        }

        saveButton.setOnClickListener(view -> saveConfiguration());
        testConnectionButton.setOnClickListener(view -> testConnection());
        reviewButton.setOnClickListener(view -> reviewTask());
    }

    private void saveConfiguration() {
        ProviderConfig config = validateProvider();
        if (config == null) {
            return;
        }
        try {
            String newKey = fieldValue(apiKeyInput);
            boolean providerChanged = !config.baseUrl.equals(configStore.getBaseUrl());
            // A blank key is retained only for the same provider. Changing endpoint without
            // entering a key removes the old key to avoid sending it to a new server.
            configStore.save(config.baseUrl, config.model, newKey, !newKey.isEmpty() || providerChanged);
            if (!newKey.isEmpty()) {
                apiKeyInput.setText("");
                apiKeyInput.setHint("Clé chiffrée enregistrée — saisissez une nouvelle valeur pour la remplacer");
            }
            setStatus(getString(R.string.status_saved));
            Toast.makeText(this, R.string.status_saved, Toast.LENGTH_SHORT).show();
        } catch (Exception exception) {
            showFailure("Impossible d’enregistrer la clé sur cet appareil.");
        }
    }

    private void testConnection() {
        ProviderConfig config = validateProvider();
        if (config == null || busy) {
            return;
        }
        runNetworkTask(getString(R.string.status_testing), () -> {
            String apiKey = resolveApiKey(config.baseUrl);
            aiClient.test(config.baseUrl, config.model, apiKey, getString(R.string.system_instruction));
            return getString(R.string.connection_success) + " Modèle : " + config.model;
        });
    }

    private void reviewTask() {
        ProviderConfig config = validateProvider();
        String task = fieldValue(taskInput);
        if (config == null || busy) {
            return;
        }
        if (task.isEmpty()) {
            taskInput.setError(getString(R.string.error_missing_task));
            taskInput.requestFocus();
            return;
        }
        taskInput.setError(null);
        runNetworkTask(getString(R.string.status_thinking), () -> {
            String apiKey = resolveApiKey(config.baseUrl);
            return aiClient.review(config.baseUrl, config.model, apiKey,
                    getString(R.string.system_instruction), task);
        });
    }

    private void runNetworkTask(String status, NetworkTask task) {
        busy = true;
        setBusy(true);
        setStatus(status);
        networkExecutor.execute(() -> {
            try {
                String result = task.run();
                runOnUiThread(() -> {
                    responseText.setText(result);
                    setStatus(getString(R.string.status_ready));
                    busy = false;
                    setBusy(false);
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    String message = exception.getMessage();
                    showFailure(message == null || message.trim().isEmpty()
                            ? getString(R.string.error_network) : message.trim());
                    busy = false;
                    setBusy(false);
                });
            }
        });
    }

    private String resolveApiKey(String requestedBaseUrl) throws Exception {
        String visibleKey = fieldValue(apiKeyInput);
        if (!visibleKey.isEmpty()) {
            return visibleKey;
        }
        // A stored credential is valid only for the endpoint it was saved with.
        return requestedBaseUrl.equals(configStore.getBaseUrl()) ? configStore.getApiKey() : "";
    }

    private ProviderConfig validateProvider() {
        String baseUrl = fieldValue(baseUrlInput);
        String model = fieldValue(modelInput);
        boolean valid = true;
        if (!OpenAiCompatibleClient.isValidBaseUrl(baseUrl)) {
            baseUrlInput.setError(getString(R.string.error_invalid_url));
            valid = false;
        } else {
            baseUrlInput.setError(null);
        }
        if (model.isEmpty()) {
            modelInput.setError(getString(R.string.error_missing_model));
            valid = false;
        } else {
            modelInput.setError(null);
        }
        return valid ? new ProviderConfig(baseUrl, model) : null;
    }

    private void setBusy(boolean isBusy) {
        saveButton.setEnabled(!isBusy);
        testConnectionButton.setEnabled(!isBusy);
        reviewButton.setEnabled(!isBusy);
    }

    private void setStatus(String value) {
        statusText.setText(value);
    }

    private void showFailure(String detail) {
        responseText.setText(detail);
        setStatus(getString(R.string.status_ready));
    }

    private String fieldValue(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    @Override
    protected void onDestroy() {
        networkExecutor.shutdownNow();
        super.onDestroy();
    }

    private interface NetworkTask {
        String run() throws Exception;
    }

    private static final class ProviderConfig {
        private final String baseUrl;
        private final String model;

        private ProviderConfig(@NonNull String baseUrl, @NonNull String model) {
            this.baseUrl = baseUrl;
            this.model = model;
        }
    }
}
