package com.lml.control;

public final class LocalAssistant {
    private LocalAssistant() {
    }

    public static String reply(String query, int allowedApps, int scenarios) {
        String normalized = query == null ? "" : query.trim().toLowerCase();
        if (normalized.isEmpty()) {
            return "Décrivez votre objectif. Je peux expliquer les permissions, les applications autorisées, les scénarios et les rappels.";
        }
        if (normalized.contains("permission") || normalized.contains("bulle")) {
            return "La bulle doit être activée par vous dans les réglages Android. Elle reste visible et peut être arrêtée depuis l’application à tout moment.";
        }
        if (normalized.contains("application") || normalized.contains("app")) {
            return "Vous avez autorisé " + allowedApps + " application(s). Ajoutez ou retirez-les dans Applications autorisées avant de créer un scénario.";
        }
        if (normalized.contains("apprend") || normalized.contains("entrain") || normalized.contains("scénario")) {
            return "L’apprentissage local consiste à enregistrer vos scénarios : application, objectif et étapes. J’en conserve actuellement " + scenarios + ". Je ne collecte pas silencieusement le contenu des autres applications.";
        }
        if (normalized.contains("tâche") || normalized.contains("objectif") || normalized.contains("rappel")) {
            return "Créez un scénario puis programmez un rappel. Le rappel vous prévient ; il n’exécute pas seul une action dans une autre application.";
        }
        if (normalized.contains("jeu")) {
            return "Je peux vous aider à structurer un objectif de jeu sous votre supervision, mais sans contourner les règles du jeu ou automatiser des actions interdites.";
        }
        return "Je fonctionne localement et sous supervision. Commencez par choisir les applications, enregistrer un scénario, puis demandez un rappel ou une explication.";
    }
}
