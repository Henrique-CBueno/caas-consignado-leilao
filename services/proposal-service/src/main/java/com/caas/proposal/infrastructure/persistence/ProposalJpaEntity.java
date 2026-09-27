package com.caas.proposal.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "proposals")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ProposalJpaEntity {

    @Id
    private UUID id;

    private UUID tenantId;
    private String borrowerId;
    private BigDecimal requestedAmount;
    private int termMonths;
    private String status;
}
