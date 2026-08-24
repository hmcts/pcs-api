-- Group access: capacity stamping and claim-creator tracking.
-- (draft_case_data.organisation_id and its index came from V018; claim drafts reuse the column.)

-- Profile decides the access type, denormalised onto the party: the organisation table isn't
-- populated at claim creation, and group access must work from the first draft.
ALTER TABLE public.party ADD COLUMN organisation_profile_id varchar(255);

-- One shared claim draft per org per case+event (the V018 index includes party_id, so it
-- constrains nothing once party_id is null). A pre-existing duplicate rolls this migration
-- back; remedy: NULL organisation_id on older duplicates (keep highest id), re-deploy.
CREATE UNIQUE INDEX draft_case_data_organisation_key
    ON draft.draft_case_data (case_reference, event_id, organisation_id)
    WHERE organisation_id IS NOT NULL AND party_id IS NULL;

-- Identifies the claimant during the draft phase, before any claim party/role exists.
ALTER TABLE public.party ADD COLUMN claim_creator boolean NOT NULL DEFAULT false;

-- Only the claimant has been given an organisation so far, so that seeds the flag.
UPDATE public.party SET claim_creator = true WHERE organisation_id IS NOT NULL;
