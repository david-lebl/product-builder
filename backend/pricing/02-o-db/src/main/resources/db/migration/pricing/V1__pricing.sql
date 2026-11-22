-- Pricing bounded context: active pricelists, B2B customer agreements, promo codes, production cost sheets.

CREATE TABLE pricing.pricelists (
    currency    TEXT        PRIMARY KEY,
    id          TEXT        NOT NULL,
    version     TEXT        NOT NULL,
    doc         JSONB       NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE pricing.customer_agreements (
    target_kind TEXT        NOT NULL CHECK (target_kind IN ('SpecificCustomer', 'TierDefault')),
    target_ref  TEXT        NOT NULL,
    currency    TEXT        NOT NULL,
    position    BIGINT      GENERATED ALWAYS AS IDENTITY,
    doc         JSONB       NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (target_kind, target_ref, currency)
);

CREATE TABLE pricing.promo_codes (
    id          TEXT        PRIMARY KEY,
    code_upper  TEXT        NOT NULL,
    is_active   BOOLEAN     NOT NULL,
    doc         JSONB       NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX promo_codes_code_uidx ON pricing.promo_codes (code_upper);

CREATE TABLE pricing.cost_sheets (
    currency    TEXT        PRIMARY KEY,
    doc         JSONB       NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
