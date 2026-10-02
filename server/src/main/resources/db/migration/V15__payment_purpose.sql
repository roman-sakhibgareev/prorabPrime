-- What a client's payment went towards: the contract, or the extra works. What exists is for the contract.
ALTER TABLE payments
    ADD COLUMN purpose VARCHAR(10) NOT NULL DEFAULT 'CONTRACT' CHECK (purpose IN ('CONTRACT', 'EXTRAS'));
ALTER TABLE payment_history
    ADD COLUMN purpose VARCHAR(10) NOT NULL DEFAULT 'CONTRACT';
