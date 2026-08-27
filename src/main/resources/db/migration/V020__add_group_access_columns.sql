-- Group access: capacity stamping and claim-creator tracking.

-- Profile decides the access type; denormalised onto the party so group access works
-- from the first draft, before the organisation table is populated.
ALTER TABLE public.party ADD COLUMN organisation_profile_id varchar(255);

-- One shared claim draft per org per case+event. The V018 index includes party_id,
-- so it constrains nothing once party_id is null.
CREATE UNIQUE INDEX draft_case_data_organisation_key
    ON draft.draft_case_data (case_reference, event_id, organisation_id)
    WHERE organisation_id IS NOT NULL AND party_id IS NULL;

-- Identifies the claimant during the draft phase, before any claim party/role exists.
ALTER TABLE public.party ADD COLUMN claim_creator boolean NOT NULL DEFAULT false;

-- Only the claimant has been given an organisation so far, so that seeds the flag.
UPDATE public.party SET claim_creator = true WHERE organisation_id IS NOT NULL;
