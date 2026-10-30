CREATE TABLE sdp_claim_snapshot (
    claim_id UUID PRIMARY KEY REFERENCES claim(id) ON DELETE CASCADE,
    snapshot_version INTEGER NOT NULL,
    snapshot_hash TEXT NOT NULL,
    exported_at TIMESTAMP NOT NULL
);
