-- Catalog bounded context: the active product catalog and compatibility ruleset.
-- Both are versioned aggregates (catalogVersion bump on every admin edit) stored as JSONB
-- documents, with an append-only history of every published version for audit / rollback.

CREATE TABLE catalog.documents (
    name        TEXT        PRIMARY KEY CHECK (name IN ('catalog', 'ruleset')),
    version     TEXT        NOT NULL,
    doc         JSONB       NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE catalog.document_history (
    id          BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        TEXT        NOT NULL,
    version     TEXT        NOT NULL,
    doc         JSONB       NOT NULL,
    saved_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX document_history_name_idx ON catalog.document_history (name, id DESC);
