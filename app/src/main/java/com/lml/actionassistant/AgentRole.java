package com.lml.actionassistant;

/**
 * Roles used by the supervised multi-brain review. Roles only analyse text and
 * never receive capabilities to control another application or device feature.
 */
public enum AgentRole {
    PLANNER("Planificateur", "Propose un chemin court, réversible et vérifiable."),
    CRITIC("Contradicteur", "Recherche les hypothèses fragiles, risques et alternatives."),
    SAFETY("Sécurité", "Identifie les données sensibles, conséquences irréversibles et confirmations nécessaires."),
    SYNTHESIZER("Synthèse", "Présente les points d’accord, désaccords et contrôles manuels.");

    private final String label;
    private final String purpose;

    AgentRole(String label, String purpose) {
        this.label = label;
        this.purpose = purpose;
    }

    public String getLabel() {
        return label;
    }

    public String getPurpose() {
        return purpose;
    }
}
