package com.caas.credit.infrastructure.decision.jev;

import java.util.Map;

public record ChoiceAnswer(String type, String choice, double confidence, Map<String, Double> probabilities) {
}
