package com.lml.actionassistant;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Immutable result returned by the on-demand, supervised multi-brain review. */
public final class MultiBrainResult {
    private final Map<AgentRole, String> opinions;
    private final String synthesis;
    private final boolean humanConfirmationRecommended;

    public MultiBrainResult(Map<AgentRole, String> opinions, String synthesis,
                            boolean humanConfirmationRecommended) {
        this.opinions = Collections.unmodifiableMap(new EnumMap<>(opinions));
        this.synthesis = synthesis;
        this.humanConfirmationRecommended = humanConfirmationRecommended;
    }

    public String getOpinion(AgentRole role) {
        String opinion = opinions.get(role);
        return opinion == null ? "Aucun avis disponible." : opinion;
    }

    public String getSynthesis() {
        return synthesis;
    }

    public boolean isHumanConfirmationRecommended() {
        return humanConfirmationRecommended;
    }
}
