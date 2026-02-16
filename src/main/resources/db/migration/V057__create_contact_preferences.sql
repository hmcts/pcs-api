CREATE TYPE contact_preference_type AS ENUM (
    'EMAIL',
    'TEXT',
    'POST',
    'PHONE'
);

CREATE TABLE contact_preference (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    party_id UUID NOT NULL,
    preference_type contact_preference_type NOT NULL,
    enabled BOOLEAN,

    CONSTRAINT fk_contact_preference_party
        FOREIGN KEY (party_id)
        REFERENCES party(id)
        ON DELETE CASCADE,

    CONSTRAINT contact_preference_party_type_unique
        UNIQUE (party_id, preference_type)
);

CREATE INDEX idx_contact_preference_party_enabled
    ON contact_preference(party_id, enabled)
    INCLUDE (preference_type);
