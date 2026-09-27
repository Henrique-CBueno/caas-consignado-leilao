package com.caas.credit.infrastructure.decision.jev;

import java.util.Map;

public record DecisionsRequest(String model, Map<String, Object> state, Map<String, ChoiceQuestion> questions) {
}
