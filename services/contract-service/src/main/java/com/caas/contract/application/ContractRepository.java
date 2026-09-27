package com.caas.contract.application;

import com.caas.contract.domain.Contract;

public interface ContractRepository {
    Contract save(Contract contract);
}
