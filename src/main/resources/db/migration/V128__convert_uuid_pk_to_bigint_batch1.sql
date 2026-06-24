-- HDPI-7336: Convert internal-only UUID primary keys to BIGINT IDENTITY (batch 1).
--
-- Pre-go-live cutover migration: ALL non-reference case data is cleared down up
-- front by the single TRUNCATE below, then primary keys are re-typed. There is
-- NO backfill - this is only valid before go-live while no live cases exist. The
-- application entities use GenerationType.IDENTITY so the database now owns id
-- generation. Clearing down here means every batch-1 and batch-2 PK swap runs on
-- guaranteed-empty tables (cascade-only truncation would leave parent tables such
-- as statement_of_truth / help_with_fees / contact_preferences / flag_ref_data
-- populated, since CASCADE only reaches child rows).
--
-- Reference/lookup data REQUIRED for claim creation is deliberately preserved:
--   postcode_court_mapping  (court allocation + eligibility lookups)
--   eligibility_whitelisted_epim
-- Scheduler infrastructure (scheduled_tasks) is owned by db-scheduler and left
-- untouched.
--
-- Batch 1 re-types: address, rent_arrears, notice_of_possession,
--   possession_alternatives, asb_prohibited_conduct, tenancy_licence.

-- ---------------------------------------------------------------------------
-- Full clear-down of every case-data table (CASCADE makes FK order irrelevant
-- and catches any child not listed). The two reference tables above are the
-- only persistent tables intentionally excluded. New bigint IDENTITY columns
-- added below start at 1 on the now-empty tables.
-- ---------------------------------------------------------------------------
TRUNCATE TABLE
    pcs_case, party, claim, counter_claim, general_application, enf_case, document,
    legal_representative, address,
    claim_ground, claim_party, claim_document, claim_party_legal_representative,
    case_flag, case_party_flag, case_link, case_link_reason, flag_ref_data,
    statement_of_truth, help_with_fees, contact_preferences,
    defendant_response, payment_agreement, household_circumstances,
    regular_income, regular_income_item, regular_expenses, reasonable_adjustments,
    party_attribute_assertion, counter_claim_party,
    fee_payment, case_note, party_access_code, claim_activity_log, case_notification,
    rent_arrears, notice_of_possession, possession_alternatives,
    asb_prohibited_conduct, tenancy_licence,
    enf_warrant, enf_writ, enf_writ_of_restitution, enf_warrant_of_restitution,
    enf_risk_profile, enf_selected_defendants,
    draft.draft_case_data
    CASCADE;

-- ---------------------------------------------------------------------------
-- address  (referenced by pcs_case.property_address_id, party.address_id,
--           legal_representative.address_id, party.contact_address_id)
-- ---------------------------------------------------------------------------
-- DROP COLUMN ... CASCADE also drops the inbound FK constraints referencing
-- address.id. The referencing columns themselves remain and are re-typed below.
ALTER TABLE address DROP COLUMN id CASCADE;
ALTER TABLE address ADD COLUMN id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY;

-- party.contact_address_id is a dead column (added in V012, never mapped by any
-- entity). DROP COLUMN id CASCADE above already removed its fk_contact_address
-- constraint; drop the now-orphaned column rather than re-typing an unused field.
ALTER TABLE party DROP COLUMN contact_address_id;

ALTER TABLE pcs_case             ALTER COLUMN property_address_id TYPE BIGINT USING NULL::BIGINT;
ALTER TABLE party                ALTER COLUMN address_id          TYPE BIGINT USING NULL::BIGINT;
ALTER TABLE legal_representative ALTER COLUMN address_id          TYPE BIGINT USING NULL::BIGINT;

ALTER TABLE pcs_case
    ADD CONSTRAINT fk_pcs_case_property_address
    FOREIGN KEY (property_address_id) REFERENCES address (id);
ALTER TABLE party
    ADD CONSTRAINT fk_party_address
    FOREIGN KEY (address_id) REFERENCES address (id);
ALTER TABLE legal_representative
    ADD CONSTRAINT fk_legal_representative_address
    FOREIGN KEY (address_id) REFERENCES address (id);

-- ---------------------------------------------------------------------------
-- Leaf claim child tables (no inbound foreign keys reference their id)
-- ---------------------------------------------------------------------------
ALTER TABLE rent_arrears DROP COLUMN id CASCADE;
ALTER TABLE rent_arrears ADD COLUMN id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY;

ALTER TABLE notice_of_possession DROP COLUMN id CASCADE;
ALTER TABLE notice_of_possession ADD COLUMN id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY;

ALTER TABLE possession_alternatives DROP COLUMN id CASCADE;
ALTER TABLE possession_alternatives ADD COLUMN id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY;

ALTER TABLE asb_prohibited_conduct DROP COLUMN id CASCADE;
ALTER TABLE asb_prohibited_conduct ADD COLUMN id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY;

ALTER TABLE tenancy_licence DROP COLUMN id CASCADE;
ALTER TABLE tenancy_licence ADD COLUMN id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY;
