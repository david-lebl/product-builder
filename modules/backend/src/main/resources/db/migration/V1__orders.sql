CREATE TABLE orders (
    id               UUID PRIMARY KEY,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    customer_name    TEXT          NOT NULL,
    customer_email   TEXT          NOT NULL,
    customer_phone   TEXT          NOT NULL,
    customer_company TEXT,
    configuration    JSONB         NOT NULL,
    price_breakdown  JSONB         NOT NULL,
    currency         TEXT          NOT NULL,
    total_amount     NUMERIC(12,2) NOT NULL,
    status           TEXT          NOT NULL CHECK (status IN ('Placed'))
);

CREATE INDEX orders_created_at_idx ON orders (created_at DESC);
