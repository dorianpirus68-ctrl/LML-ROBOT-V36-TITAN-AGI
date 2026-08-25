package com.lml.actionassistant;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Runs several specialised text reviews and produces a final human-reviewed synthesis.
 * This class has no scheduling, background persistence, device-control, or app-control capability.
 */
public final class MultiBrainOrchestrator {
    private static final String COLLECTIVE_RULES = "Vous participez à un collectif d’assistance supervisé. "
            + "Répondez en français. Analysez uniquement le texte reçu. Vous ne contrôlez aucune "
            + "application, vous n’exécutez aucune action et vous ne prétendez jamais avoir agi. "
            + "Refusez tout contournement de sécurité ou de règles de service. Soyez concis, concret "
            + "et indiquez les conséquences et vérifications manuelles nécessaires.";

    private final OpenAiCompatibleClient client;

    public MultiBrainOrchestrator(OpenAiCompatibleClient client) {
        this.client = client;
    }

    public MultiBrainResult review(String baseUrl, String model, String apiKey, String userTask)
            throws Exception {
        Map<AgentRole, String> opinions = new EnumMap<>(AgentRole.class);
        int successfulReviews = 0;

        String planner = requestRole(baseUrl, model, apiKey, AgentRole.PLANNER, userTask);
        opinions.put(AgentRole.PLANNER, planner);
        successfulReviews += isAvailable(planner) ? 1 : 0;

        String critic = requestRole(baseUrl, model, apiKey, AgentRole.CRITIC, userTask);
        opinions.put(AgentRole.CRITIC, critic);
        successfulReviews += isAvailable(critic) ? 1 : 0;

        String safety = requestRole(baseUrl, model, apiKey, AgentRole.SAFETY, userTask);
        opinions.put(AgentRole.SAFETY, safety);
        successfulReviews += isAvailable(safety) ? 1 : 0;

        if (successfulReviews == 0) {
            throw new IllegalStateException("Aucun cerveau n’a pu obtenir une réponse du fournisseur.");
        }

        String synthesis = synthesize(baseUrl, model, apiKey, userTask, opinions);
        opinions.put(AgentRole.SYNTHESIZER, synthesis);
        return new MultiBrainResult(opinions, synthesis, requiresHumanConfirmation(safety));
    }

    private String requestRole(String baseUrl, String model, String apiKey, AgentRole role, String task) {
        try {
            return client.review(baseUrl, model, apiKey, roleInstruction(role), task);
        } catch (Exception exception) {
            return "Analyse indisponible pour ce rôle. Vérifiez la connexion ou réessayez manuellement.";
        }
    }

    private String synthesize(String baseUrl, String model, String apiKey, String task,
                              Map<AgentRole, String> opinions) throws Exception {
        StringBuilder context = new StringBuilder();
        context.append("Demande initiale :\n").append(task).append("\n\n");
        context.append("Avis du planificateur :\n").append(opinions.get(AgentRole.PLANNER)).append("\n\n");
        context.append("Avis du contradicteur :\n").append(opinions.get(AgentRole.CRITIC)).append("\n\n");
        context.append("Avis de sécurité :\n").append(opinions.get(AgentRole.SAFETY));

        String instruction = COLLECTIVE_RULES + "\n\nVous êtes le cerveau Synthèse. À partir des avis fournis, "
                + "présentez : 1) la recommandation la plus prudente, 2) les points de désaccord, "
                + "3) les vérifications à effectuer manuellement, 4) les conséquences ou confirmations "
                + "à considérer. Ne masquez pas une réserve de sécurité et n’inventez aucune information.";
        try {
            return client.review(baseUrl, model, apiKey, instruction, context.toString());
        } catch (Exception exception) {
            return "La synthèse automatique est indisponible. Consultez les avis des trois cerveaux "
                    + "et décidez manuellement après vérification.";
        }
    }

    private String roleInstruction(AgentRole role) {
        switch (role) {
            case PLANNER:
                return COLLECTIVE_RULES + "\n\nVous êtes le cerveau Planificateur. Proposez un objectif reformulé, "
                        + "puis trois à sept étapes courtes, réversibles et vérifiables. Distinguez les prérequis "
                        + "des étapes. Terminez par les éléments que l’utilisateur doit faire lui-même.";
            case CRITIC:
                return COLLECTIVE_RULES + "\n\nVous êtes le cerveau Contradicteur. Recherchez les hypothèses "
                        + "fragiles, les erreurs possibles, les alternatives et ce qui manque pour conclure. "
                        + "N’appelez pas à l’action : formulez des contrôles manuels concrets.";
            case SAFETY:
                return COLLECTIVE_RULES + "\n\nVous êtes le cerveau Sécurité. Commencez par « NIVEAU : FAIBLE », "
                        + "« NIVEAU : MODÉRÉ » ou « NIVEAU : ÉLEVÉ ». Identifiez les données à ne pas partager, "
                        + "les conséquences irréversibles, les règles à respecter et les confirmations explicites nécessaires.";
            default:
                return COLLECTIVE_RULES;
        }
    }

    private boolean isAvailable(String opinion) {
        return opinion != null && !opinion.startsWith("Analyse indisponible");
    }

    private boolean requiresHumanConfirmation(String safetyOpinion) {
        String normalized = safetyOpinion == null ? "" : safetyOpinion.toLowerCase(Locale.ROOT);
        return normalized.contains("niveau : élevé")
                || normalized.contains("irréversible")
                || normalized.contains("donnée sensible")
                || normalized.contains("confirmation explicite");
    }
}
