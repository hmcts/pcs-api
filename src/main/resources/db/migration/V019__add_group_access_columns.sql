-- Group access: what capacity an organisation's case is stamped with, and who created the claim.
--
-- The draft.draft_case_data.organisation_id column and its index are NOT declared here: V018 added
-- them for the legal representative journey, and this ticket keys claim drafts on the same column.

-- The profile decides which access type an organisation gets, so it decides the capacity the case
-- is stamped with. Denormalised onto the party rather than read through the organisation table:
-- that table is populated by the legal representative journey, which has not run at claim creation,
-- and group access has to work from the first draft.
ALTER TABLE public.party ADD COLUMN organisation_profile_id varchar(255);

-- draft_case_data_unique_by_organisation_idx does not cover claim drafts: it includes party_id, and
-- Postgres counts NULLs as distinct, so it constrains nothing once party_id is null.
CREATE UNIQUE INDEX draft_case_data_organisation_key
    ON draft.draft_case_data (case_reference, event_id, organisation_id)
    WHERE organisation_id IS NOT NULL AND party_id IS NULL;

-- Identifies the claimant during the draft phase, which is when group access has to start working.
-- Claim parties only exist once the claim is submitted, so until then there is no role to read.
ALTER TABLE public.party ADD COLUMN claim_creator boolean NOT NULL DEFAULT false;

-- Until now only the claimant was given an organisation, so that is what seeds the flag.
UPDATE public.party SET claim_creator = true WHERE organisation_id IS NOT NULL;
