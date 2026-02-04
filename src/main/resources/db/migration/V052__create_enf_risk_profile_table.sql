CREATE TYPE YES_NO_NOT_SURE AS ENUM ('YES', 'NO', 'NOT_SURE');

CREATE TYPE VULNERABLE_CATEGORY AS ENUM (
    'VULNERABLE_ADULTS',
    'VULNERABLE_CHILDREN',
    'VULNERABLE_ADULTS_AND_CHILDREN'
);

CREATE TABLE enf_risk_profile (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    enf_case_id UUID NOT NULL REFERENCES enf_case(id) ON DELETE CASCADE,
    any_risk_to_bailiff YES_NO_NOT_SURE,
    vulnerable_people_present YES_NO_NOT_SURE,
    vulnerable_category VULNERABLE_CATEGORY,
    vulnerable_reason_text TEXT,
    violent_details TEXT,
    firearms_details TEXT,
    criminal_details TEXT,
    verbal_threats_details TEXT,
    protest_group_details TEXT,
    police_social_services_details TEXT,
    animals_details TEXT,
    CONSTRAINT unique_risk_profile_per_case UNIQUE(enf_case_id)
);

CREATE INDEX idx_enf_risk_profile_case_id ON enf_risk_profile(enf_case_id);
