package com.caas.events;

import java.util.UUID;

public record CreditDecisionMadeEvent(
    UUID proposalId,
    UUID tenantId,
    String decision,
    double confidence
) {
}
