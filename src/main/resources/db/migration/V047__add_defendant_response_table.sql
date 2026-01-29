CREATE TABLE defendant_response (
                    id UUID PRIMARY KEY,
                    claim_id UUID NOT NULL REFERENCES claim(id),
                    party_id UUID NOT NULL REFERENCES party(id),
                    possession_order_type VARCHAR(30)
);
