-- Group access: the organisation that owns a case, and the draft on it.

-- The profile decides which access type an organisation gets, so it decides the capacity the case
-- is stamped with.
ALTER TABLE public.party ADD COLUMN organisation_profile_ids varchar(255)[];

-- A draft on a claim belongs to the organisation that owns the case rather than the person typing
-- it. When null the draft stays keyed on the user, which is how the defendant response journey keeps
-- a citizen and their legal representative separate.
--
-- The organisation rather than the claimant party, so the draft is keyed on the same thing the
-- case's CaseAccessGroups are, and two claimant parties in one organisation share a draft rather
-- than being ambiguous. No foreign key: case_reference, idam_user_id and party_id carry none either.
ALTER TABLE draft.draft_case_data ADD COLUMN organisation_id varchar(64);

-- One draft per organisation per event. The pre-existing draft_case_data_unique_idx does not cover
-- these rows: it includes party_id, and Postgres treats NULLs as distinct.
CREATE UNIQUE INDEX draft_case_data_organisation_key
    ON draft.draft_case_data (case_reference, event_id, organisation_id)
    WHERE organisation_id IS NOT NULL;
