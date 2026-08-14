-- Group access: the organisation that owns a case, and the draft on it.

-- The profile decides which access type an organisation gets, so it decides the capacity the case
-- is stamped with.
ALTER TABLE public.party ADD COLUMN organisation_profile_id varchar(255);

-- A draft on a claim belongs to the organisation that owns the case rather than the person typing
-- it. When null the draft stays keyed on the user, which is how the defendant response journey keeps
-- a citizen and their legal representative separate. The organisation rather than the claimant party
-- so it matches what CaseAccessGroups are derived from. No foreign key, as on the columns beside it.
ALTER TABLE draft.draft_case_data ADD COLUMN organisation_id varchar(64);

-- draft_case_data_unique_idx does not cover these rows: it includes party_id, and Postgres counts
-- NULLs as distinct, so it constrains nothing once party_id is null.
CREATE UNIQUE INDEX draft_case_data_organisation_key
    ON draft.draft_case_data (case_reference, event_id, organisation_id)
    WHERE organisation_id IS NOT NULL;

-- Identifies the claimant during the draft phase, which is when group access has to start working.
-- Claim parties only exist once the claim is submitted, so until then there is no role to read.
ALTER TABLE public.party ADD COLUMN claim_creator boolean NOT NULL DEFAULT false;

-- Until now only the claimant was given an organisation, so that is what seeds the flag.
UPDATE public.party SET claim_creator = true WHERE organisation_id IS NOT NULL;
