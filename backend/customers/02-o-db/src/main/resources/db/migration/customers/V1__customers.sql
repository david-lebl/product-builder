-- Customers bounded context: customer accounts (B2C + B2B organizations).
-- Lookup columns (email, normalized IČO / DIČ, status, tier) are indexed; the aggregate is JSONB.

CREATE TABLE customers.customers (
    id                  TEXT        PRIMARY KEY,
    email               TEXT        NOT NULL,
    reg_no_normalized   TEXT,
    vat_id_normalized   TEXT,
    segment             TEXT        NOT NULL,
    status              TEXT        NOT NULL,
    tier                TEXT        NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    doc                 JSONB       NOT NULL
);
CREATE UNIQUE INDEX customers_email_uidx ON customers.customers (email);
CREATE INDEX customers_reg_no_idx ON customers.customers (reg_no_normalized);
CREATE INDEX customers_vat_id_idx ON customers.customers (vat_id_normalized);
CREATE INDEX customers_status_idx ON customers.customers (status);
