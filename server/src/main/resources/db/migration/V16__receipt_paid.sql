-- Whether a receipt has been paid for. Apart from the sum, so it can be marked before the sum is known.
ALTER TABLE photos ADD COLUMN receipt_paid BOOLEAN NOT NULL DEFAULT FALSE;
