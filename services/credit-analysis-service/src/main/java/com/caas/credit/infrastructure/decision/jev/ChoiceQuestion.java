package com.caas.credit.infrastructure.decision.jev;

import java.util.Map;

public record ChoiceQuestion(String type, String instructions, Map<String, String> criteria) {
}
