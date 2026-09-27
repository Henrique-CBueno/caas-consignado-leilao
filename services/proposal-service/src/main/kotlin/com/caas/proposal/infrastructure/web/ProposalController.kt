package com.caas.proposal.infrastructure.web

import com.caas.proposal.application.CreateProposalCommand
import com.caas.proposal.application.CreateProposalUseCase
import com.caas.proposal.application.ProposalRepository
import com.caas.proposal.domain.Proposal
import com.caas.proposal.domain.ProposalId
import com.caas.proposal.infrastructure.TenantContextHolder
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.util.UUID

data class CreateProposalRequest(
    val borrowerId: String,
    val requestedAmount: BigDecimal,
    val termMonths: Int,
)

data class CreateProposalResponse(
    val id: UUID,
    val borrowerId: String,
    val requestedAmount: BigDecimal,
    val termMonths: Int,
    val status: String,
)

@RestController
class ProposalController(
    private val proposalRepository: ProposalRepository,
    private val createProposalUseCase: CreateProposalUseCase,
) {

    @PostMapping("/proposals")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: CreateProposalRequest): CreateProposalResponse {
        if (request.requestedAmount <= BigDecimal.ZERO) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "requestedAmount deve ser maior que zero")
        }
        if (request.termMonths <= 0) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "termMonths deve ser maior que zero")
        }
        val tenantId = TenantContextHolder.get() ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)

        val saved = createProposalUseCase.execute(
            CreateProposalCommand(
                tenantId = tenantId,
                borrowerId = request.borrowerId,
                requestedAmount = request.requestedAmount,
                termMonths = request.termMonths,
            ),
        )
        return saved.toResponse()
    }

    @GetMapping("/proposals/{id}")
    fun getById(@PathVariable id: UUID): ResponseEntity<CreateProposalResponse> {
        val proposal = proposalRepository.findById(ProposalId(id)) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(proposal.toResponse())
    }

    private fun Proposal.toResponse() =
        CreateProposalResponse(id.value, borrowerId, requestedAmount, termMonths, status.name)
}
