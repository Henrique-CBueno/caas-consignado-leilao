package com.caas.contract.application;

import com.caas.contract.domain.Contract;
import com.caas.contract.domain.ContractCorrelation;
import com.caas.contract.domain.ContractId;
import com.caas.contract.domain.ProposalId;
import com.caas.contract.domain.TenantId;
import com.caas.events.ContractSignedEvent;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContractGenerationService {

    private final ContractRepository contractRepository;
    private final ContractCorrelationRepository correlationRepository;
    private final OutboxEventPublisher outboxEventPublisher;
    private final EntityManager entityManager;

    public ContractGenerationService(
        ContractRepository contractRepository,
        ContractCorrelationRepository correlationRepository,
        OutboxEventPublisher outboxEventPublisher,
        EntityManager entityManager
    ) {
        this.contractRepository = contractRepository;
        this.correlationRepository = correlationRepository;
        this.outboxEventPublisher = outboxEventPublisher;
        this.entityManager = entityManager;
    }

    @Transactional
    public void onCreditApproved(ProposalId proposalId, TenantId tenantId, BigDecimal requestedAmount) {
        lockCorrelation(proposalId);
        ContractCorrelation correlation = loadOrCreate(proposalId, tenantId).withCreditApproved(requestedAmount);
        correlationRepository.save(correlation);
        tryGenerateContract(correlation);
    }

    @Transactional
    public void onAuctionWon(ProposalId proposalId, TenantId tenantId, String funderId, BigDecimal rate, Integer termMonths) {
        lockCorrelation(proposalId);
        ContractCorrelation correlation =
            loadOrCreate(proposalId, tenantId).withAuctionWinner(funderId, rate, termMonths);
        correlationRepository.save(correlation);
        tryGenerateContract(correlation);
    }

    // pg_advisory_xact_lock, não SELECT ... FOR UPDATE: a trava de linha não protege
    // a primeira escrita (linha ainda não existe para travar) — os dois listeners
    // (crédito e leilão) podem tentar criar a correlação ao mesmo tempo e colidir
    // na constraint única. O advisory lock serializa por proposalId mesmo quando a
    // linha ainda não existe; liberado automaticamente ao fim da transação.
    private void lockCorrelation(ProposalId proposalId) {
        entityManager
            .createNativeQuery("SELECT pg_advisory_xact_lock(:key)")
            .setParameter("key", proposalId.value().getMostSignificantBits())
            .getSingleResult();
    }

    private ContractCorrelation loadOrCreate(ProposalId proposalId, TenantId tenantId) {
        ContractCorrelation existing = correlationRepository.findByProposalIdForUpdate(proposalId);
        return existing != null ? existing : ContractCorrelation.empty(proposalId, tenantId);
    }

    private void tryGenerateContract(ContractCorrelation correlation) {
        if (correlation.contractGenerated() || !correlation.isComplete()) {
            return;
        }

        Contract contract = new Contract(
            new ContractId(UUID.randomUUID()),
            correlation.proposalId(),
            correlation.tenantId(),
            correlation.winningFunderId(),
            correlation.winningRate(),
            correlation.winningTermMonths(),
            correlation.requestedAmount(),
            Instant.now()
        );
        contractRepository.save(contract);
        correlationRepository.save(correlation.markContractGenerated());

        outboxEventPublisher.publish(
            "Contract",
            contract.proposalId().value(),
            "ContractSigned",
            new ContractSignedEvent(
                contract.proposalId().value(),
                contract.tenantId().value(),
                contract.funderId(),
                contract.rate(),
                contract.termMonths(),
                contract.amount(),
                contract.signedAt()
            )
        );
    }
}
