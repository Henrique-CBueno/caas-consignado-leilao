package com.caas.proposal.application

import com.caas.events.ProposalCreatedEvent
import com.caas.proposal.domain.Proposal
import com.caas.proposal.domain.ProposalId
import com.caas.proposal.domain.TenantId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

data class CreateProposalCommand(
    val tenantId: TenantId,
    val borrowerId: String,
    val requestedAmount: BigDecimal,
    val termMonths: Int,
)

@Service
class CreateProposalUseCase(
    private val proposalRepository: ProposalRepository,
    private val outboxEventPublisher: OutboxEventPublisher,
) {
    // @Transactional aqui é o que faz a gravação da proposta e do evento de outbox
    // andarem na mesma transação: os dois adapters usam propagação REQUIRED (padrão),
    // então entram na transação já aberta por este método em vez de abrir a própria.
    @Transactional
    fun execute(command: CreateProposalCommand): Proposal {
        val proposal = Proposal(
            id = ProposalId(UUID.randomUUID()),
            tenantId = command.tenantId,
            borrowerId = command.borrowerId,
            requestedAmount = command.requestedAmount,
            termMonths = command.termMonths,
        )
        val saved = proposalRepository.save(proposal)

        outboxEventPublisher.publish(
            aggregateType = "Proposal",
            aggregateId = saved.id.value,
            eventType = "ProposalCreated",
            payload = ProposalCreatedEvent(
                proposalId = saved.id.value,
                tenantId = saved.tenantId.value,
                borrowerId = saved.borrowerId,
                requestedAmount = saved.requestedAmount,
                termMonths = saved.termMonths,
            ),
        )

        return saved
    }
}
