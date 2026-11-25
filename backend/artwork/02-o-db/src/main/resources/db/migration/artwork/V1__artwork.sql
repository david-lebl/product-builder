-- Artwork bounded context: artwork submissions with immutable revision history & preflight reports.
-- Binary print files live in BlobStorage (local FS / S3); rows only hold metadata + storage keys.

CREATE TABLE artwork.submissions (
    id               TEXT        PRIMARY KEY,
    owner_kind       TEXT        NOT NULL CHECK (owner_kind IN ('DraftSession', 'OrderLineRef')),
    owner_ref        TEXT        NOT NULL,
    owner_line_index INT,
    component_key    TEXT        NOT NULL,
    status           TEXT        NOT NULL,
    revision_count   INT         NOT NULL,
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    doc              JSONB       NOT NULL
);
CREATE INDEX submissions_owner_idx ON artwork.submissions (owner_kind, owner_ref, owner_line_index);
CREATE INDEX submissions_status_idx ON artwork.submissions (status);
