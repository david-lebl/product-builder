-- Identity bounded context: password credentials (customers + staff), OTP challenges, sessions.
-- Key/query columns are relational & indexed; the full aggregate lives in `doc` (JSONB).

CREATE TABLE identity.password_credentials (
    email           TEXT        PRIMARY KEY,
    principal_kind  TEXT        NOT NULL CHECK (principal_kind IN ('Customer', 'Employee')),
    principal_ref   TEXT        NOT NULL,
    is_active       BOOLEAN     NOT NULL,
    doc             JSONB       NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX password_credentials_principal_idx ON identity.password_credentials (principal_kind, principal_ref);

CREATE TABLE identity.otp_challenges (
    id          TEXT        PRIMARY KEY,
    expires_at  TIMESTAMPTZ NOT NULL,
    consumed    BOOLEAN     NOT NULL,
    doc         JSONB       NOT NULL
);
CREATE INDEX otp_challenges_expires_idx ON identity.otp_challenges (expires_at);

CREATE TABLE identity.sessions (
    token           TEXT        PRIMARY KEY,
    principal_kind  TEXT        NOT NULL,
    principal_ref   TEXT        NOT NULL,
    expires_at      TIMESTAMPTZ NOT NULL,
    revoked         BOOLEAN     NOT NULL,
    doc             JSONB       NOT NULL
);
CREATE INDEX sessions_principal_idx ON identity.sessions (principal_kind, principal_ref);
CREATE INDEX sessions_expires_idx ON identity.sessions (expires_at);
