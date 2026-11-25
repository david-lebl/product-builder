-- Ordering bounded context: carts, orders (+ normalized, append-only status audit trail),
-- and store delivery / payment settings (pickup locations, couriers, bank accounts).

CREATE TABLE ordering.carts (
    id           TEXT        PRIMARY KEY,
    customer_id  TEXT,
    currency     TEXT        NOT NULL,
    doc          JSONB       NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX carts_customer_idx ON ordering.carts (customer_id);

-- `doc` holds the order aggregate WITHOUT `statusHistory`; history lives in order_status_events.
CREATE TABLE ordering.orders (
    id              TEXT        PRIMARY KEY,
    order_number    TEXT        NOT NULL,
    customer_id     TEXT,
    customer_email  TEXT        NOT NULL,
    status          TEXT        NOT NULL,
    payment_status  TEXT        NOT NULL,
    currency        TEXT        NOT NULL,
    grand_total     NUMERIC(14, 2),
    placed_at       TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    doc             JSONB       NOT NULL
);
CREATE UNIQUE INDEX orders_number_uidx ON ordering.orders (order_number);
CREATE INDEX orders_customer_idx ON ordering.orders (customer_id);
CREATE INDEX orders_status_idx ON ordering.orders (status);
CREATE INDEX orders_placed_at_idx ON ordering.orders (placed_at DESC);

-- Immutable status timeline (Accepted, OnHold, InProduction, …) — insert-only.
CREATE TABLE ordering.order_status_events (
    order_id     TEXT        NOT NULL REFERENCES ordering.orders (id) ON DELETE CASCADE,
    seq          INT         NOT NULL,
    to_status    TEXT        NOT NULL,
    occurred_at  TIMESTAMPTZ NOT NULL,
    doc          JSONB       NOT NULL,
    PRIMARY KEY (order_id, seq)
);
CREATE INDEX order_status_events_occurred_idx ON ordering.order_status_events (occurred_at DESC);

CREATE TABLE ordering.pickup_locations (
    id         TEXT        PRIMARY KEY,
    position   BIGINT      GENERATED ALWAYS AS IDENTITY,
    doc        JSONB       NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE ordering.courier_options (
    currency   TEXT        NOT NULL,
    id         TEXT        NOT NULL,
    position   BIGINT      GENERATED ALWAYS AS IDENTITY,
    doc        JSONB       NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (currency, id)
);

CREATE TABLE ordering.bank_accounts (
    currency   TEXT        PRIMARY KEY,
    doc        JSONB       NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
