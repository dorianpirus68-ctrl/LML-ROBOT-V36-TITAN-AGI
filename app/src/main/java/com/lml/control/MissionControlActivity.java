package com.lml.control;

import androidx.fragment.app.FragmentActivity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;

public class MissionControlActivity extends FragmentActivity {
    private EditText objective;
    private TextView planText;
    private AgentPolicy.Plan plan;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);
        root.setBackgroundColor(Color.rgb(6, 15, 29));

        TextView title = text("MISSION CONTROL", 26, Color.rgb(91, 245, 255));
        root.addView(title);
        TextView help = text("Décrivez un objectif. LML construit un plan local, le classe puis exige une validation. Les opérations critiques restent bloquées sans gestionnaire spécialisé.", 16, Color.WHITE);
        help.setPadding(0, 14, 0, 18);
        root.addView(help);

        objective = new EditText(this);
        objective.setHint("Ex. Ouvrir l’application autorisée et faire défiler une fois");
        objective.setTextColor(Color.WHITE);
        objective.setHintTextColor(Color.LTGRAY);
        root.addView(objective, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Button planButton = new Button(this);
        planButton.setText("1. Générer un plan local");
        planButton.setOnClickListener(v -> generatePlan());
        root.addView(planButton);

        planText = text("Aucun plan actif.", 16, Color.rgb(205, 245, 255));
        planText.setPadding(0, 18, 0, 18);
        root.addView(planText);

        Button stageOne = new Button(this);
        stageOne.setText("2. Approuver le plan (45 secondes)");
        stageOne.setOnClickListener(v -> approveStageOne());
        root.addView(stageOne);

        Button stageTwo = new Button(this);
        stageTwo.setText("3. Revalider avant exécution");
        stageTwo.setOnClickListener(v -> revalidateStageTwo());
        root.addView(stageTwo);

        TextView note = text("Le modèle n’appelle jamais directement une action Android. Chaque navigation reste restreinte aux applications autorisées et à l’exécuteur contrôlé de LML-Control.", 14, Color.rgb(255, 205, 90));
        note.setPadding(0, 18, 0, 0);
        root.addView(note);
        setContentView(root);
    }

    private TextView text(String content, int size, int color) {
        TextView view = new TextView(this);
        view.setText(content);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private void generatePlan() {
        plan = AgentPolicy.propose(objective.getText().toString(), AllowedAppsActivity.getAllowedCount(this), TrainingActivity.getScenarioCount(this));
        NexusAuditLog.record(this, "plan_generated", plan.actionType.name());
        planText.setText(plan.summary());
    }

    private void approveStageOne() {
        if (plan == null) { Toast.makeText(this, "Générez d’abord un plan", Toast.LENGTH_SHORT).show(); return; }
        long expiry = DualApprovalGate.approveStageOne(this, plan, 45_000L);
        planText.append("\n\nÉTAPE 1 VALIDÉE jusqu’à " + android.text.format.DateFormat.format("HH:mm:ss", expiry));
    }

    private void revalidateStageTwo() {
        if (plan == null || !DualApprovalGate.consumeStageTwo(this, plan)) { Toast.makeText(this, "Ticket absent, expiré ou déjà utilisé", Toast.LENGTH_LONG).show(); return; }
        if (plan.risk == AgentPolicy.Risk.CRITICAL) {
            requestStrongConfirmation();
        } else if (plan.risk == AgentPolicy.Risk.CONTROLLED) {
            planText.append("\n\nÉTAPE 2 VALIDÉE : ouvrez Actions approuvées pour exécuter la navigation contrôlée.");
        } else {
            planText.append("\n\nÉTAPE 2 VALIDÉE : conseil local consigné, aucune action externe demandée.");
        }
    }

    private void requestStrongConfirmation() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt prompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                planText.append("\n\nVÉRIFICATION FORTE RÉUSSIE : aucune exécution critique n’est disponible dans cette version.");
            }
            @Override public void onAuthenticationError(int code, CharSequence message) { super.onAuthenticationError(code, message); planText.append("\n\nVÉRIFICATION ANNULÉE : " + message); }
        });
        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Validation critique LML")
                .setSubtitle("Confirmez votre identité avant toute action critique")
                .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG | androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();
        prompt.authenticate(info);
    }
}
