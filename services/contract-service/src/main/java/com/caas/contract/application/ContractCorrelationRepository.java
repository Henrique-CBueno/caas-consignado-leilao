package com.caas.contract.application;

import com.caas.contract.domain.ContractCorrelation;
import com.caas.contract.domain.ProposalId;

public interface ContractCorrelationRepository {
    // Chamado sempre dentro da transação que já tomou o advisory lock por
    // proposalId (ver ContractGenerationService) — sem isso, os dois listeners
    // (crédito e leilão) processando a mesma proposta quase ao mesmo tempo podem
    // colidir tentando criar a mesma linha de correlação.
    ContractCorrelation findByProposalIdForUpdate(ProposalId proposalId);

    ContractCorrelation save(ContractCorrelation correlation);
}
