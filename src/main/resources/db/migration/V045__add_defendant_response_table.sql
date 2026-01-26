CREATE TABLE defendant_response (
                    id UUID PRIMARY KEY,
                    claim_id UUID NOT NULL REFERENCES claim(id),
                    party_id UUID NOT NULL REFERENCES party(id),
                    response_type  VARCHAR(50) NOT NULL,
                    possession_order_type VARCHAR(30),
                    defence_submitted BOOLEAN NOT NULL,
                    counterclaim_submitted BOOLEAN NOT NULL,
                    created_at TIMESTAMP DEFAULT NOW(),
                    updated_at TIMESTAMP DEFAULT NOW()
);
