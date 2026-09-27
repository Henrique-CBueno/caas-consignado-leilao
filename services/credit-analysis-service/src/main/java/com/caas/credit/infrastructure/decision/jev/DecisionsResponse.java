package com.caas.credit.infrastructure.decision.jev;

import java.util.Map;

public record DecisionsResponse(String id, String model, String provider, Map<String, ChoiceAnswer> answers) {
}
