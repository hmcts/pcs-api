CREATE TYPE YES_NO_NOT_SURE AS ENUM ('YES', 'NO', 'NOT_SURE');

CREATE TABLE enf_risk_profile (
    id UUID PRIMARY KEY,
    enf_case_id UUID NOT NULL REFERENCES enf_case(id) ON DELETE CASCADE,
    any_risk_to_bailiff YES_NO_NOT_SURE,
    vulnerable_people_present YES_NO_NOT_SURE,
    vulnerable_category VARCHAR(100),
    vulnerable_reason_text VARCHAR(6400),
    violent_details VARCHAR(6400),
    firearms_details VARCHAR(6400),
    criminal_details VARCHAR(6400),
    verbal_threats_details VARCHAR(6400),
    protest_group_details VARCHAR(6400),
    police_social_services_details VARCHAR(6400),
    animals_details VARCHAR(6400),
    CONSTRAINT unique_risk_profile_per_case UNIQUE(enf_case_id)
);

CREATE INDEX idx_enf_risk_profile_case_id ON enf_risk_profile(enf_case_id);
