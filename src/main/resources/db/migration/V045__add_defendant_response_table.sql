CREATE TABLE defendant_response (
                    id UUID PRIMARY KEY,
                    claim_id UUID REFERENCES claim(id),
                    party_id UUID REFERENCES party(id),
                    response_type  VARCHAR(50) NOT NULL,
                    possession_order_type VARCHAR(30),
                    defence_submitted BOOLEAN NOT NULL,
                    counterclaim_submitted BOOLEAN NOT NULL,
                    additional_information VARCHAR(6800),
                    --statement_of_truth_id REFERENCES
                    created_at TIMESTAMP Default NOW(),
                    updated_at TIMESTAMP Default NOW()
);

CREATE INDEX idx_defendant_response_id ON defendant_response(id);
