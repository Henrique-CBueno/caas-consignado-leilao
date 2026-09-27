package com.caas.credit.infrastructure.decision;

import com.caas.credit.application.CreditDecisionPort;
import com.caas.credit.application.CreditDecisionRequest;
import com.caas.credit.domain.CreditDecisionResult;
import com.caas.credit.domain.Decision;
import org.springframework.stereotype.Component;

// Sem dependência externa (nenhuma chamada de rede): score simulado via hash
// estável do borrowerId. String.hashCode() no Java é garantido determinístico
// pela especificação da linguagem (mesmo algoritmo sempre, entre JVMs/execuções).
@Component
public class MockDecisionAdapter implements CreditDecisionPort {

    @Override
    public CreditDecisionResult decide(CreditDecisionRequest request) {
        int score = simulatedScore(request.borrowerId());
        if (score >= 700) {
            return new CreditDecisionResult(Decision.APPROVE, 0.9);
        }
        if (score >= 400) {
            // Confiança propositalmente abaixo do threshold (0.6, ver CreditDecisionService)
            // para exercitar a trava de MANUAL_REVIEW na faixa intermediária.
            return new CreditDecisionResult(Decision.APPROVE, 0.5);
        }
        return new CreditDecisionResult(Decision.REJECT, 0.85);
    }

    private int simulatedScore(String borrowerId) {
        return Math.abs(borrowerId.hashCode()) % 1000;
    }
}
