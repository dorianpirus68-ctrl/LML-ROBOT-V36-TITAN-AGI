package com.lml.control;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/** Deterministic policy. A language model may suggest an objective but cannot bypass this classifier. */
public final class AgentPolicy {
    public enum Risk { LOW, CONTROLLED, CRITICAL }
    public enum ActionType { LOCAL_ADVICE, APP_OPEN, NAVIGATE_BACK, NAVIGATE_SCROLL, BLOCKED }

    public static final class Plan {
        public final String action;
        public final ActionType actionType;
        public final Risk risk;
        public final String explanation;
        public final String hash;

        private Plan(String action, ActionType actionType, Risk risk, String explanation) {
            this.action = action;
            this.actionType = actionType;
            this.risk = risk;
            this.explanation = explanation;
            this.hash = sha256(action + "|" + actionType.name() + "|" + risk.name() + "|" + explanation);
        }

        public String summary() {
            return "ACTION : " + action + "\nTYPE : " + actionType.name() + "\nRISQUE : " + risk.name() + "\n" + explanation + "\nEMPREINTE : " + hash.substring(0, 16);
        }
    }

    private AgentPolicy() { }

    public static Plan propose(String objective, int allowedApps, int scenarios) {
        String normalized = objective == null ? "" : objective.toLowerCase(Locale.ROOT).trim();
        if (normalized.matches(".*(payer|achat|virement|supprim|effacer|envoyer|publier|compte|mot de passe|installer).*")) {
            return new Plan("Action critique bloquée", ActionType.BLOCKED, Risk.CRITICAL,
                    "Cette demande requiert un gestionnaire spécialisé. Aucun exécuteur critique n’est disponible dans cette version.");
        }
        if (normalized.matches(".*(retour|revenir).*")) return navigationPlan("Retour supervisé", ActionType.NAVIGATE_BACK, allowedApps, scenarios);
        if (normalized.matches(".*(défiler|scroll).*")) return navigationPlan("Défilement supervisé", ActionType.NAVIGATE_SCROLL, allowedApps, scenarios);
        if (normalized.matches(".*(ouvrir|lancer).*")) return navigationPlan("Ouverture d’application autorisée", ActionType.APP_OPEN, allowedApps, scenarios);
        return new Plan("Conseil ou préparation locale", ActionType.LOCAL_ADVICE, Risk.LOW,
                "Le noyau peut préparer un scénario, expliquer une capacité ou proposer une étape. Applications autorisées : " + allowedApps + " ; scénarios structurés : " + scenarios + ".");
    }

    private static Plan navigationPlan(String action, ActionType actionType, int allowedApps, int scenarios) {
        if (allowedApps <= 0) return new Plan("Navigation refusée", ActionType.BLOCKED, Risk.CONTROLLED,
                "Aucune application n’est autorisée. Ajoutez d’abord une application à la liste locale.");
        return new Plan(action, actionType, Risk.CONTROLLED,
                "Le plan est limité à une application autorisée, à une action courte et à un ticket à usage unique. Scénarios structurés : " + scenarios + ".");
    }

    private static String sha256(String input) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte value : bytes) result.append(String.format(Locale.ROOT, "%02x", value));
            return result.toString();
        } catch (NoSuchAlgorithmException error) { return Integer.toHexString(input.hashCode()); }
    }
}
