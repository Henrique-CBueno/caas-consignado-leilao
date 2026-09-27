-- Necessário para a revalidação pós-leilão (Milestone 8): o credit-analysis-service
-- precisa recarregar borrowerId/requestedAmount da decisão pré-leilão sem chamar
-- o proposal-service de novo, e distinguir decisão pré- de pós-leilão.
ALTER TABLE credit_decisions ADD COLUMN borrower_id TEXT NOT NULL DEFAULT '';
ALTER TABLE credit_decisions ADD COLUMN requested_amount NUMERIC NOT NULL DEFAULT 0;
ALTER TABLE credit_decisions ADD COLUMN stage TEXT NOT NULL DEFAULT 'PRE_AUCTION';
