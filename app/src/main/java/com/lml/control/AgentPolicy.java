package com.lml.control;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public final class AgentPolicy {
    public enum Risk { LOW, CONTROLLED, CRITICAL }

    public static final class Plan {
        public final String action;
        public final Risk risk;
        public final String explanation;
        public final String hash;

        private Plan(String action, Risk risk, String explanation) {
            this.action = action;
            this.risk = risk;
            this.explanation = explanation;
            this.hash = sha256(action + "|" + risk.name() + "|" + explanation);
        }
    }

    private AgentPolicy() { }

    public static Plan propose(String objective, int allowedApps, int scenarios) {
        String normalized = objective == null ? "" : objective.toLowerCase(Locale.ROOT).trim();
        if (normalized.matches(".*(payer|achat|virement|supprim|effacer|envoyer|publier|compte|mot de passe).*")) {
            return new Plan("Action critique bloquée", Risk.CRITICAL,
                    "Cette demande requiert une validation forte et un gestionnaire spécifique. Aucun exécuteur critique n’est activé dans cette version.");
        }
        if (normalized.matches(".*(ouvrir|retour|revenir|défiler|scroll|naviguer).*")) {
            return new Plan("Navigation supervisée", Risk.CONTROLLED,
                    "Le plan peut utiliser uniquement une application autorisée et une navigation courte après votre validation.");
        }
        return new Plan("Conseil ou préparation locale", Risk.LOW,
                "L’agent peut préparer un scénario, expliquer les capacités et proposer une étape. Applications autorisées : "
                        + allowedApps + " ; scénarios : " + scenarios + ".");
    }

    private static String sha256(String input) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte value : bytes) result.append(String.format(Locale.ROOT, "%02x", value));
            return result.toString();
        } catch (NoSuchAlgorithmException error) {
            return Integer.toHexString(input.hashCode());
        }
    }
}
