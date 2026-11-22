-- Platform-owned objects shared by BFF facades.
-- Monotonic sequence behind human-friendly IDs (ord-N, art-N, line-N, ORD-YYYY-NNNN).
-- Starts high so it never collides with seeded sample data (ord-1 … ord-4, art-1 … art-4).
CREATE SEQUENCE IF NOT EXISTS platform.id_sequence START WITH 10000 INCREMENT BY 1;
