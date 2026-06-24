-- HDPI-7336: Convert internal-only UUID primary keys to BIGINT IDENTITY (batch 2).
--
-- Pre-go-live cutover migration: case data is intentionally cleared down and primary
-- keys are re-typed. NO backfill - only valid before go-live while no live cases exist.
-- Entities use GenerationType.IDENTITY so the database owns id generation.
--
-- Out of scope (remain UUID per ticket / CCD exposure):
--   pcs_case, party, claim, counter_claim, general_application, enf_case,
--   document, claim_ground, case_flag, case_party_flag, case_link, case_link_reason,
--   legal_representative, flag_ref_data.
-- Embedded external UUIDs left untouched: legal_representative.idam_id,
--   party_access_code.party_id, fee_payment.related_entity_id (general_application),
--   party_attribute_assertion.evidence_document_id (document),
--   case_notification.provider_notification_id (Gov Notify), draft.draft_case_data
--   idam_user_id / party_id.

-- ===========================================================================
-- 0. Clear-down (whole case graph cascades from pcs_case; draft is separate)
-- ===========================================================================
TRUNCATE TABLE pcs_case CASCADE;
TRUNCATE TABLE draft.draft_case_data CASCADE;

-- ===========================================================================
-- 1. Primary-key swaps: uuid -> bigint identity
--    DROP COLUMN id CASCADE also drops the inbound FK constraints referencing
--    each id; the referencing columns are re-typed in section 2.
-- ===========================================================================
ALTER TABLE statement_of_truth        DROP COLUMN id CASCADE;
ALTER TABLE statement_of_truth        ADD COLUMN id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY;
ALTER TABLE help_with_fees            DROP COLUMN id CASCADE;
ALTER TABLE help_with_fees            ADD COLUMN id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY;
ALTER TABLE fee_payment               DROP COLUMN id CASCADE;
ALTER TABLE fee_payment               ADD COLUMN id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY;
ALTER TABLE defendant_response        DROP COLUMN id CASCADE;
ALTER TABLE defendant_response        ADD COLUMN id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY;
ALTER TABLE payment_agreement         DROP COLUMN id CASCADE;
ALTER TABLE payment_agreement         ADD COLUMN id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY;
ALTER TABLE household_circumstances   DROP COLUMN id CASCADE;
ALTER TABLE household_circumstances   ADD COLUMN id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY;
ALTER TABLE regular_income            DROP COLUMN id CASCADE;
ALTER TABLE regular_income            ADD COLUMN id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY;
ALTER TABLE regular_income_item       DROP COLUMN id CASCADE;
ALTER TABLE regular_income_item       ADD COLUMN id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY;
ALTER TABLE regular_expenses          DROP COLUMN id CASCADE;
ALTER TABLE regular_expenses          ADD COLUMN id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY;
ALTER TABLE reasonable_adjustments    DROP COLUMN id CASCADE;
ALTER TABLE reasonable_adjustments    ADD COLUMN id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY;
ALTER TABLE party_attribute_assertion DROP COLUMN id CASCADE;
ALTER TABLE party_attribute_assertion ADD COLUMN id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY;
ALTER TABLE counter_claim_party       DROP COLUMN id CASCADE;
ALTER TABLE counter_claim_party       ADD COLUMN id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY;
ALTER TABLE contact_preferences       DROP COLUMN id CASCADE;
ALTER TABLE contact_preferences       ADD COLUMN id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY;
ALTER TABLE case_note                 DROP COLUMN id CASCADE;
ALTER TABLE case_note                 ADD COLUMN id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY;
ALTER TABLE party_access_code         DROP COLUMN id CASCADE;
ALTER TABLE party_access_code         ADD COLUMN id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY;
ALTER TABLE claim_activity_log        DROP COLUMN id CASCADE;
ALTER TABLE claim_activity_log        ADD COLUMN id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY;
ALTER TABLE case_notification         DROP COLUMN id CASCADE;
ALTER TABLE case_notification         ADD COLUMN id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY;
ALTER TABLE draft.draft_case_data     DROP COLUMN id CASCADE;
ALTER TABLE draft.draft_case_data     ADD COLUMN id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY;
ALTER TABLE enf_warrant               DROP COLUMN id CASCADE;
ALTER TABLE enf_warrant               ADD COLUMN id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY;
ALTER TABLE enf_writ                  DROP COLUMN id CASCADE;
ALTER TABLE enf_writ                  ADD COLUMN id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY;
ALTER TABLE enf_writ_of_restitution   DROP COLUMN id CASCADE;
ALTER TABLE enf_writ_of_restitution   ADD COLUMN id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY;
ALTER TABLE enf_warrant_of_restitution DROP COLUMN id CASCADE;
ALTER TABLE enf_warrant_of_restitution ADD COLUMN id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY;
ALTER TABLE enf_risk_profile          DROP COLUMN id CASCADE;
ALTER TABLE enf_risk_profile          ADD COLUMN id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY;
ALTER TABLE enf_selected_defendants   DROP COLUMN id CASCADE;
ALTER TABLE enf_selected_defendants   ADD COLUMN id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY;

-- ===========================================================================
-- 2. Re-type inbound foreign-key columns (parents converted above)
-- ===========================================================================
-- -> statement_of_truth
ALTER TABLE general_application ALTER COLUMN sot_id TYPE BIGINT USING NULL::BIGINT;
ALTER TABLE counter_claim       ALTER COLUMN sot_id TYPE BIGINT USING NULL::BIGINT;
ALTER TABLE defendant_response  ALTER COLUMN sot_id TYPE BIGINT USING NULL::BIGINT;
ALTER TABLE enf_case            ALTER COLUMN sot_id TYPE BIGINT USING NULL::BIGINT;
-- -> help_with_fees
ALTER TABLE general_application ALTER COLUMN hwf_id TYPE BIGINT USING NULL::BIGINT;
ALTER TABLE fee_payment         ALTER COLUMN hwf_id TYPE BIGINT USING NULL::BIGINT;
-- -> defendant_response
ALTER TABLE payment_agreement       ALTER COLUMN defendant_response_id TYPE BIGINT USING NULL::BIGINT;
ALTER TABLE reasonable_adjustments  ALTER COLUMN defendant_response_id TYPE BIGINT USING NULL::BIGINT;
ALTER TABLE household_circumstances ALTER COLUMN defendant_response_id TYPE BIGINT USING NULL::BIGINT;
ALTER TABLE document                ALTER COLUMN defendant_response_id TYPE BIGINT USING NULL::BIGINT;
-- -> household_circumstances
ALTER TABLE regular_income   ALTER COLUMN hc_id TYPE BIGINT USING NULL::BIGINT;
ALTER TABLE regular_expenses ALTER COLUMN hc_id TYPE BIGINT USING NULL::BIGINT;
-- -> regular_income
ALTER TABLE regular_income_item ALTER COLUMN regular_income_id TYPE BIGINT USING NULL::BIGINT;
-- -> contact_preferences
ALTER TABLE party ALTER COLUMN contact_preferences_id TYPE BIGINT USING NULL::BIGINT;

-- ===========================================================================
-- 3. Re-create foreign-key constraints
-- ===========================================================================
ALTER TABLE general_application ADD CONSTRAINT fk_genapp_sot               FOREIGN KEY (sot_id) REFERENCES statement_of_truth (id);
ALTER TABLE counter_claim       ADD CONSTRAINT fk_counter_claim_sot        FOREIGN KEY (sot_id) REFERENCES statement_of_truth (id);
ALTER TABLE defendant_response  ADD CONSTRAINT fk_defendant_response_sot   FOREIGN KEY (sot_id) REFERENCES statement_of_truth (id);
ALTER TABLE enf_case            ADD CONSTRAINT fk_enf_case_sot             FOREIGN KEY (sot_id) REFERENCES statement_of_truth (id);
ALTER TABLE general_application ADD CONSTRAINT fk_genapp_hwf               FOREIGN KEY (hwf_id) REFERENCES help_with_fees (id);
ALTER TABLE fee_payment         ADD CONSTRAINT fk_fee_payment_hwf          FOREIGN KEY (hwf_id) REFERENCES help_with_fees (id);
ALTER TABLE payment_agreement       ADD CONSTRAINT fk_payment_agreement_dr   FOREIGN KEY (defendant_response_id) REFERENCES defendant_response (id);
ALTER TABLE reasonable_adjustments  ADD CONSTRAINT fk_reasonable_adj_dr      FOREIGN KEY (defendant_response_id) REFERENCES defendant_response (id);
ALTER TABLE household_circumstances ADD CONSTRAINT fk_household_circ_dr      FOREIGN KEY (defendant_response_id) REFERENCES defendant_response (id);
ALTER TABLE document                ADD CONSTRAINT fk_document_dr            FOREIGN KEY (defendant_response_id) REFERENCES defendant_response (id);
ALTER TABLE regular_income   ADD CONSTRAINT fk_regular_income_hc   FOREIGN KEY (hc_id) REFERENCES household_circumstances (id);
ALTER TABLE regular_expenses ADD CONSTRAINT fk_regular_expenses_hc FOREIGN KEY (hc_id) REFERENCES household_circumstances (id);
ALTER TABLE regular_income_item ADD CONSTRAINT fk_regular_income_item_ri FOREIGN KEY (regular_income_id) REFERENCES regular_income (id);
ALTER TABLE party ADD CONSTRAINT fk_party_contact_preferences FOREIGN KEY (contact_preferences_id) REFERENCES contact_preferences (id);
