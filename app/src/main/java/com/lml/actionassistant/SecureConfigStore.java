package com.lml.actionassistant;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Stores provider configuration locally. The API key is encrypted with a hardware-backed
 * Android Keystore key when the device supports it. It is never written to logs or backups.
 */
public final class SecureConfigStore {
    private static final String PREFS_NAME = "lml_action_assistant_settings";
    private static final String KEY_ALIAS = "lml_action_assistant_api_key_v1";
    private static final String FIELD_BASE_URL = "base_url";
    private static final String FIELD_MODEL = "model";
    private static final String FIELD_ENCRYPTED_API_KEY = "encrypted_api_key";

    private final SharedPreferences preferences;

    public SecureConfigStore(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void save(String baseUrl, String model, String apiKey, boolean updateApiKey) throws Exception {
        SharedPreferences.Editor editor = preferences.edit()
                .putString(FIELD_BASE_URL, baseUrl.trim())
                .putString(FIELD_MODEL, model.trim());

        if (updateApiKey) {
            if (apiKey == null || apiKey.trim().isEmpty()) {
                editor.remove(FIELD_ENCRYPTED_API_KEY);
            } else {
                editor.putString(FIELD_ENCRYPTED_API_KEY, encrypt(apiKey.trim()));
            }
        }
        editor.apply();
    }

    public String getBaseUrl() {
        return preferences.getString(FIELD_BASE_URL, "https://api.openai.com/v1");
    }

    public String getModel() {
        return preferences.getString(FIELD_MODEL, "gpt-4.1-mini");
    }

    public boolean hasApiKey() {
        return preferences.contains(FIELD_ENCRYPTED_API_KEY);
    }

    public String getApiKey() throws Exception {
        String payload = preferences.getString(FIELD_ENCRYPTED_API_KEY, "");
        if (payload == null || payload.isEmpty()) {
            return "";
        }
        return decrypt(payload);
    }

    private String encrypt(String value) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey());
        byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        byte[] iv = cipher.getIV();
        return "v1:" + Base64.encodeToString(iv, Base64.NO_WRAP) + ":"
                + Base64.encodeToString(encrypted, Base64.NO_WRAP);
    }

    private String decrypt(String payload) throws Exception {
        String[] parts = payload.split(":", 3);
        if (parts.length != 3 || !"v1".equals(parts[0])) {
            throw new IllegalStateException("Format de clé chiffrée invalide.");
        }
        byte[] iv = Base64.decode(parts[1], Base64.NO_WRAP);
        byte[] encrypted = Base64.decode(parts[2], Base64.NO_WRAP);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), new GCMParameterSpec(128, iv));
        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    }

    private SecretKey getOrCreateSecretKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (keyStore.containsAlias(KEY_ALIAS)) {
            return ((KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null)).getSecretKey();
        }

        KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build();
        keyGenerator.init(spec);
        return keyGenerator.generateKey();
    }
}
