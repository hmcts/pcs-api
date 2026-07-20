-- V136__r1a_integer_ids.sql
-- HDPI-7834: convert internal UUID primary keys to bigint identity (additive, pre-go-live).
-- Data is DROPPED (pre-go-live reset). Reference data (postcode_court_mapping,
-- eligibility_whitelisted_epim, flag_ref_data) and flyway history are preserved.
-- Identical on every environment: TRUNCATE is a no-op on empty prod, drops test data elsewhere.
-- Generated from the live schema: 31 id cols convert, 18 FK cols retype,
-- 11 retained uuid, 2 pending-uuid (flag_ref_data/legal_representative).

-- 1. Drop all foreign key constraints (re-added verbatim in step 5)
ALTER TABLE public.asb_prohibited_conduct DROP CONSTRAINT asb_prohibited_conduct_claim_id_fkey;
ALTER TABLE public.case_flag DROP CONSTRAINT case_flag_flag_ref_data_id_fkey;
ALTER TABLE public.case_flag DROP CONSTRAINT case_flag_pcs_case_id_fkey;
ALTER TABLE public.case_link DROP CONSTRAINT case_link_case_id_fkey;
ALTER TABLE public.case_link_reason DROP CONSTRAINT case_link_reason_case_link_id_fkey;
ALTER TABLE public.case_note DROP CONSTRAINT case_note_case_id_fkey;
ALTER TABLE public.case_notification DROP CONSTRAINT fk_case_notification_case;
ALTER TABLE public.case_notification DROP CONSTRAINT fk_case_notification_claim;
ALTER TABLE public.case_notification DROP CONSTRAINT fk_case_notification_party;
ALTER TABLE public.case_party_flag DROP CONSTRAINT case_party_flag_flag_ref_data_id_fkey;
ALTER TABLE public.case_party_flag DROP CONSTRAINT case_party_flag_party_id_fkey;
ALTER TABLE public.claim DROP CONSTRAINT claim_case_id_fkey;
ALTER TABLE public.claim DROP CONSTRAINT claim_claim_form_document_id_fkey;
ALTER TABLE public.claim_activity_log DROP CONSTRAINT claim_activity_log_case_id_fkey;
ALTER TABLE public.claim_activity_log DROP CONSTRAINT claim_activity_log_party_id_fkey;
ALTER TABLE public.claim_document DROP CONSTRAINT claim_document_claim_id_fkey;
ALTER TABLE public.claim_document DROP CONSTRAINT claim_document_document_id_fkey;
ALTER TABLE public.claim_ground DROP CONSTRAINT claim_ground_claim_id_fkey;
ALTER TABLE public.claim_party DROP CONSTRAINT claim_party_claim_id_fkey;
ALTER TABLE public.claim_party DROP CONSTRAINT claim_party_party_id_fkey;
ALTER TABLE public.claim_party_legal_representative DROP CONSTRAINT claim_party_legal_representative_legal_representative_id_fkey;
ALTER TABLE public.claim_party_legal_representative DROP CONSTRAINT claim_party_legal_representative_party_id_fkey;
ALTER TABLE public.counter_claim DROP CONSTRAINT counter_claim_case_id_fkey;
ALTER TABLE public.counter_claim DROP CONSTRAINT counter_claim_party_id_fkey;
ALTER TABLE public.counter_claim DROP CONSTRAINT counter_claim_sot_id_fkey;
ALTER TABLE public.counter_claim_party DROP CONSTRAINT counter_claim_party_cc_id_fkey;
ALTER TABLE public.counter_claim_party DROP CONSTRAINT counter_claim_party_party_id_fkey;
ALTER TABLE public.defendant_response DROP CONSTRAINT defendant_response_claim_id_fkey;
ALTER TABLE public.defendant_response DROP CONSTRAINT defendant_response_party_id_fkey;
ALTER TABLE public.defendant_response DROP CONSTRAINT defendant_response_pcs_case_id_fkey;
ALTER TABLE public.defendant_response DROP CONSTRAINT defendant_response_sot_id_fkey;
ALTER TABLE public.defendant_response DROP CONSTRAINT defendant_response_submission_document_id_fkey;
ALTER TABLE public.document DROP CONSTRAINT document_case_id_fkey;
ALTER TABLE public.document DROP CONSTRAINT document_claim_id_fkey;
ALTER TABLE public.document DROP CONSTRAINT document_counter_claim_id_fkey;
ALTER TABLE public.document DROP CONSTRAINT document_defendant_response_id_fkey;
ALTER TABLE public.document DROP CONSTRAINT document_enf_case_id_fkey;
ALTER TABLE public.document DROP CONSTRAINT document_general_application_id_fkey;
ALTER TABLE public.document DROP CONSTRAINT document_party_id_fkey;
ALTER TABLE public.enf_case DROP CONSTRAINT enf_case_claim_id_fkey;
ALTER TABLE public.enf_case DROP CONSTRAINT enf_case_sot_id_fkey;
ALTER TABLE public.enf_risk_profile DROP CONSTRAINT enf_risk_profile_enf_case_id_fkey;
ALTER TABLE public.enf_selected_defendants DROP CONSTRAINT enf_selected_defendants_enf_case_id_fkey;
ALTER TABLE public.enf_selected_defendants DROP CONSTRAINT enf_selected_defendants_party_id_fkey;
ALTER TABLE public.enf_warrant DROP CONSTRAINT enf_warrant_enf_case_id_fkey;
ALTER TABLE public.enf_warrant_of_restitution DROP CONSTRAINT enf_warrant_of_restitution_enf_case_id_fkey;
ALTER TABLE public.enf_writ DROP CONSTRAINT enf_writ_enf_case_id_fkey;
ALTER TABLE public.enf_writ_of_restitution DROP CONSTRAINT enf_writ_of_restitution_enf_case_id_fkey;
ALTER TABLE public.fee_payment DROP CONSTRAINT fk_fee_payment_claim;
ALTER TABLE public.fee_payment DROP CONSTRAINT fk_fee_payment_help_with_fees;
ALTER TABLE public.general_application DROP CONSTRAINT fk_general_application_party_case;
ALTER TABLE public.general_application DROP CONSTRAINT general_application_case_id_fkey;
ALTER TABLE public.general_application DROP CONSTRAINT general_application_hwf_id_fkey;
ALTER TABLE public.general_application DROP CONSTRAINT general_application_party_id_fkey;
ALTER TABLE public.general_application DROP CONSTRAINT general_application_sot_id_fkey;
ALTER TABLE public.general_application DROP CONSTRAINT general_application_submission_document_id_fkey;
ALTER TABLE public.household_circumstances DROP CONSTRAINT household_circumstances_defendant_response_id_fkey;
ALTER TABLE public.legal_representative DROP CONSTRAINT legal_representative_address_id_fkey;
ALTER TABLE public.notice_of_possession DROP CONSTRAINT notice_of_possession_claim_id_fkey;
ALTER TABLE public.party DROP CONSTRAINT party_address_id_fkey;
ALTER TABLE public.party DROP CONSTRAINT party_case_id_fkey;
ALTER TABLE public.party DROP CONSTRAINT party_contact_preferences_id_fkey;
ALTER TABLE public.party_access_code DROP CONSTRAINT fk_party_access_code_party;
ALTER TABLE public.party_access_code DROP CONSTRAINT party_access_code_case_id_fkey;
ALTER TABLE public.party_attribute_assertion DROP CONSTRAINT party_attribute_assertion_created_by_fkey;
ALTER TABLE public.party_attribute_assertion DROP CONSTRAINT party_attribute_assertion_evidence_document_id_fkey;
ALTER TABLE public.party_attribute_assertion DROP CONSTRAINT party_attribute_assertion_last_updated_by_fkey;
ALTER TABLE public.party_attribute_assertion DROP CONSTRAINT party_attribute_assertion_party_id_fkey;
ALTER TABLE public.payment_agreement DROP CONSTRAINT payment_agreement_defendant_response_id_fkey;
ALTER TABLE public.pcs_case DROP CONSTRAINT pcs_case_property_address_id_fkey;
ALTER TABLE public.possession_alternatives DROP CONSTRAINT possession_alternatives_claim_id_fkey;
ALTER TABLE public.reasonable_adjustments DROP CONSTRAINT reasonable_adjustments_defendant_response_id_fkey;
ALTER TABLE public.regular_expenses DROP CONSTRAINT regular_expenses_hc_id_fkey;
ALTER TABLE public.regular_income DROP CONSTRAINT fk_regular_income_hc;
ALTER TABLE public.regular_income_item DROP CONSTRAINT fk_income_item_regular_income;
ALTER TABLE public.rent_arrears DROP CONSTRAINT rent_arrears_claim_id_fkey;
ALTER TABLE public.statement_of_truth DROP CONSTRAINT statement_of_truth_claim_id_fkey;
ALTER TABLE public.tenancy_licence DROP CONSTRAINT tenancy_licence_case_id_fkey;

-- 2. Empty the case tables (no-op on empty prod; drops disposable test data elsewhere)
TRUNCATE TABLE public.address, public.asb_prohibited_conduct, public.case_flag, public.case_link, public.case_link_reason, public.case_note, public.case_notification, public.case_party_flag, public.claim, public.claim_activity_log, public.claim_document, public.claim_ground, public.claim_party, public.claim_party_legal_representative, public.contact_preferences, public.counter_claim, public.counter_claim_party, public.defendant_response, public.document, draft.draft_case_data, public.enf_case, public.enf_risk_profile, public.enf_selected_defendants, public.enf_warrant, public.enf_warrant_of_restitution, public.enf_writ, public.enf_writ_of_restitution, public.fee_payment, public.general_application, public.help_with_fees, public.household_circumstances, public.legal_representative, public.notice_of_possession, public.party, public.party_access_code, public.party_attribute_assertion, public.payment_agreement, public.pcs_case, public.possession_alternatives, public.reasonable_adjustments, public.regular_expenses, public.regular_income, public.regular_income_item, public.rent_arrears, public.scheduled_tasks, public.statement_of_truth, public.tenancy_licence;

-- 3. Convert 31 primary keys: uuid -> bigint GENERATED ALWAYS AS IDENTITY
ALTER TABLE public.address ALTER COLUMN id TYPE bigint USING NULL::bigint;
ALTER TABLE public.address ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY;
ALTER TABLE public.asb_prohibited_conduct ALTER COLUMN id TYPE bigint USING NULL::bigint;
ALTER TABLE public.asb_prohibited_conduct ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY;
ALTER TABLE public.case_link ALTER COLUMN id TYPE bigint USING NULL::bigint;
ALTER TABLE public.case_link ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY;
ALTER TABLE public.case_note ALTER COLUMN id TYPE bigint USING NULL::bigint;
ALTER TABLE public.case_note ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY;
ALTER TABLE public.case_notification ALTER COLUMN id TYPE bigint USING NULL::bigint;
ALTER TABLE public.case_notification ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY;
ALTER TABLE public.claim_activity_log ALTER COLUMN id DROP DEFAULT;
ALTER TABLE public.claim_activity_log ALTER COLUMN id TYPE bigint USING NULL::bigint;
ALTER TABLE public.claim_activity_log ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY;
ALTER TABLE public.contact_preferences ALTER COLUMN id TYPE bigint USING NULL::bigint;
ALTER TABLE public.contact_preferences ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY;
ALTER TABLE public.counter_claim_party ALTER COLUMN id TYPE bigint USING NULL::bigint;
ALTER TABLE public.counter_claim_party ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY;
ALTER TABLE public.defendant_response ALTER COLUMN id TYPE bigint USING NULL::bigint;
ALTER TABLE public.defendant_response ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY;
ALTER TABLE draft.draft_case_data ALTER COLUMN id TYPE bigint USING NULL::bigint;
ALTER TABLE draft.draft_case_data ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY;
ALTER TABLE public.enf_risk_profile ALTER COLUMN id TYPE bigint USING NULL::bigint;
ALTER TABLE public.enf_risk_profile ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY;
ALTER TABLE public.enf_selected_defendants ALTER COLUMN id TYPE bigint USING NULL::bigint;
ALTER TABLE public.enf_selected_defendants ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY;
ALTER TABLE public.enf_warrant ALTER COLUMN id TYPE bigint USING NULL::bigint;
ALTER TABLE public.enf_warrant ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY;
ALTER TABLE public.enf_warrant_of_restitution ALTER COLUMN id TYPE bigint USING NULL::bigint;
ALTER TABLE public.enf_warrant_of_restitution ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY;
ALTER TABLE public.enf_writ ALTER COLUMN id TYPE bigint USING NULL::bigint;
ALTER TABLE public.enf_writ ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY;
ALTER TABLE public.enf_writ_of_restitution ALTER COLUMN id TYPE bigint USING NULL::bigint;
ALTER TABLE public.enf_writ_of_restitution ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY;
ALTER TABLE public.fee_payment ALTER COLUMN id TYPE bigint USING NULL::bigint;
ALTER TABLE public.fee_payment ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY;
ALTER TABLE public.help_with_fees ALTER COLUMN id TYPE bigint USING NULL::bigint;
ALTER TABLE public.help_with_fees ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY;
ALTER TABLE public.household_circumstances ALTER COLUMN id TYPE bigint USING NULL::bigint;
ALTER TABLE public.household_circumstances ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY;
ALTER TABLE public.notice_of_possession ALTER COLUMN id TYPE bigint USING NULL::bigint;
ALTER TABLE public.notice_of_possession ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY;
ALTER TABLE public.party_access_code ALTER COLUMN id TYPE bigint USING NULL::bigint;
ALTER TABLE public.party_access_code ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY;
ALTER TABLE public.party_attribute_assertion ALTER COLUMN id TYPE bigint USING NULL::bigint;
ALTER TABLE public.party_attribute_assertion ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY;
ALTER TABLE public.payment_agreement ALTER COLUMN id TYPE bigint USING NULL::bigint;
ALTER TABLE public.payment_agreement ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY;
ALTER TABLE public.possession_alternatives ALTER COLUMN id TYPE bigint USING NULL::bigint;
ALTER TABLE public.possession_alternatives ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY;
ALTER TABLE public.reasonable_adjustments ALTER COLUMN id TYPE bigint USING NULL::bigint;
ALTER TABLE public.reasonable_adjustments ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY;
ALTER TABLE public.regular_expenses ALTER COLUMN id TYPE bigint USING NULL::bigint;
ALTER TABLE public.regular_expenses ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY;
ALTER TABLE public.regular_income ALTER COLUMN id TYPE bigint USING NULL::bigint;
ALTER TABLE public.regular_income ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY;
ALTER TABLE public.regular_income_item ALTER COLUMN id TYPE bigint USING NULL::bigint;
ALTER TABLE public.regular_income_item ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY;
ALTER TABLE public.rent_arrears ALTER COLUMN id TYPE bigint USING NULL::bigint;
ALTER TABLE public.rent_arrears ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY;
ALTER TABLE public.statement_of_truth ALTER COLUMN id TYPE bigint USING NULL::bigint;
ALTER TABLE public.statement_of_truth ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY;
ALTER TABLE public.tenancy_licence ALTER COLUMN id TYPE bigint USING NULL::bigint;
ALTER TABLE public.tenancy_licence ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY;

-- 4. Retype 18 FK columns that point at converted tables: uuid -> bigint
ALTER TABLE public.legal_representative ALTER COLUMN address_id TYPE bigint USING NULL::bigint;   -- -> address
ALTER TABLE public.party ALTER COLUMN address_id TYPE bigint USING NULL::bigint;   -- -> address
ALTER TABLE public.pcs_case ALTER COLUMN property_address_id TYPE bigint USING NULL::bigint;   -- -> address
ALTER TABLE public.case_link_reason ALTER COLUMN case_link_id TYPE bigint USING NULL::bigint;   -- -> case_link
ALTER TABLE public.party ALTER COLUMN contact_preferences_id TYPE bigint USING NULL::bigint;   -- -> contact_preferences
ALTER TABLE public.document ALTER COLUMN defendant_response_id TYPE bigint USING NULL::bigint;   -- -> defendant_response
ALTER TABLE public.household_circumstances ALTER COLUMN defendant_response_id TYPE bigint USING NULL::bigint;   -- -> defendant_response
ALTER TABLE public.payment_agreement ALTER COLUMN defendant_response_id TYPE bigint USING NULL::bigint;   -- -> defendant_response
ALTER TABLE public.reasonable_adjustments ALTER COLUMN defendant_response_id TYPE bigint USING NULL::bigint;   -- -> defendant_response
ALTER TABLE public.fee_payment ALTER COLUMN hwf_id TYPE bigint USING NULL::bigint;   -- -> help_with_fees
ALTER TABLE public.general_application ALTER COLUMN hwf_id TYPE bigint USING NULL::bigint;   -- -> help_with_fees
ALTER TABLE public.regular_expenses ALTER COLUMN hc_id TYPE bigint USING NULL::bigint;   -- -> household_circumstances
ALTER TABLE public.regular_income ALTER COLUMN hc_id TYPE bigint USING NULL::bigint;   -- -> household_circumstances
ALTER TABLE public.regular_income_item ALTER COLUMN regular_income_id TYPE bigint USING NULL::bigint;   -- -> regular_income
ALTER TABLE public.counter_claim ALTER COLUMN sot_id TYPE bigint USING NULL::bigint;   -- -> statement_of_truth
ALTER TABLE public.defendant_response ALTER COLUMN sot_id TYPE bigint USING NULL::bigint;   -- -> statement_of_truth
ALTER TABLE public.enf_case ALTER COLUMN sot_id TYPE bigint USING NULL::bigint;   -- -> statement_of_truth
ALTER TABLE public.general_application ALTER COLUMN sot_id TYPE bigint USING NULL::bigint;   -- -> statement_of_truth

-- 5. Re-add all foreign key constraints
ALTER TABLE public.asb_prohibited_conduct ADD CONSTRAINT asb_prohibited_conduct_claim_id_fkey FOREIGN KEY (claim_id) REFERENCES claim(id);
ALTER TABLE public.case_flag ADD CONSTRAINT case_flag_flag_ref_data_id_fkey FOREIGN KEY (flag_ref_data_id) REFERENCES flag_ref_data(id) ON DELETE CASCADE;
ALTER TABLE public.case_flag ADD CONSTRAINT case_flag_pcs_case_id_fkey FOREIGN KEY (pcs_case_id) REFERENCES pcs_case(id) ON DELETE CASCADE;
ALTER TABLE public.case_link ADD CONSTRAINT case_link_case_id_fkey FOREIGN KEY (case_id) REFERENCES pcs_case(id) ON DELETE CASCADE;
ALTER TABLE public.case_link_reason ADD CONSTRAINT case_link_reason_case_link_id_fkey FOREIGN KEY (case_link_id) REFERENCES case_link(id) ON DELETE CASCADE;
ALTER TABLE public.case_note ADD CONSTRAINT case_note_case_id_fkey FOREIGN KEY (case_id) REFERENCES pcs_case(id);
ALTER TABLE public.case_notification ADD CONSTRAINT fk_case_notification_case FOREIGN KEY (case_id) REFERENCES pcs_case(id);
ALTER TABLE public.case_notification ADD CONSTRAINT fk_case_notification_claim FOREIGN KEY (claim_id) REFERENCES claim(id);
ALTER TABLE public.case_notification ADD CONSTRAINT fk_case_notification_party FOREIGN KEY (party_id) REFERENCES party(id);
ALTER TABLE public.case_party_flag ADD CONSTRAINT case_party_flag_flag_ref_data_id_fkey FOREIGN KEY (flag_ref_data_id) REFERENCES flag_ref_data(id) ON DELETE CASCADE;
ALTER TABLE public.case_party_flag ADD CONSTRAINT case_party_flag_party_id_fkey FOREIGN KEY (party_id) REFERENCES party(id) ON DELETE CASCADE;
ALTER TABLE public.claim ADD CONSTRAINT claim_case_id_fkey FOREIGN KEY (case_id) REFERENCES pcs_case(id);
ALTER TABLE public.claim ADD CONSTRAINT claim_claim_form_document_id_fkey FOREIGN KEY (claim_form_document_id) REFERENCES document(id);
ALTER TABLE public.claim_activity_log ADD CONSTRAINT claim_activity_log_case_id_fkey FOREIGN KEY (case_id) REFERENCES pcs_case(id);
ALTER TABLE public.claim_activity_log ADD CONSTRAINT claim_activity_log_party_id_fkey FOREIGN KEY (party_id) REFERENCES party(id);
ALTER TABLE public.claim_document ADD CONSTRAINT claim_document_claim_id_fkey FOREIGN KEY (claim_id) REFERENCES claim(id);
ALTER TABLE public.claim_document ADD CONSTRAINT claim_document_document_id_fkey FOREIGN KEY (document_id) REFERENCES document(id);
ALTER TABLE public.claim_ground ADD CONSTRAINT claim_ground_claim_id_fkey FOREIGN KEY (claim_id) REFERENCES claim(id);
ALTER TABLE public.claim_party ADD CONSTRAINT claim_party_claim_id_fkey FOREIGN KEY (claim_id) REFERENCES claim(id);
ALTER TABLE public.claim_party ADD CONSTRAINT claim_party_party_id_fkey FOREIGN KEY (party_id) REFERENCES party(id);
ALTER TABLE public.claim_party_legal_representative ADD CONSTRAINT claim_party_legal_representative_legal_representative_id_fkey FOREIGN KEY (legal_representative_id) REFERENCES legal_representative(id);
ALTER TABLE public.claim_party_legal_representative ADD CONSTRAINT claim_party_legal_representative_party_id_fkey FOREIGN KEY (party_id) REFERENCES party(id);
ALTER TABLE public.counter_claim ADD CONSTRAINT counter_claim_case_id_fkey FOREIGN KEY (case_id) REFERENCES pcs_case(id);
ALTER TABLE public.counter_claim ADD CONSTRAINT counter_claim_party_id_fkey FOREIGN KEY (party_id) REFERENCES party(id);
ALTER TABLE public.counter_claim ADD CONSTRAINT counter_claim_sot_id_fkey FOREIGN KEY (sot_id) REFERENCES statement_of_truth(id);
ALTER TABLE public.counter_claim_party ADD CONSTRAINT counter_claim_party_cc_id_fkey FOREIGN KEY (cc_id) REFERENCES counter_claim(id);
ALTER TABLE public.counter_claim_party ADD CONSTRAINT counter_claim_party_party_id_fkey FOREIGN KEY (party_id) REFERENCES party(id);
ALTER TABLE public.defendant_response ADD CONSTRAINT defendant_response_claim_id_fkey FOREIGN KEY (claim_id) REFERENCES claim(id);
ALTER TABLE public.defendant_response ADD CONSTRAINT defendant_response_party_id_fkey FOREIGN KEY (party_id) REFERENCES party(id);
ALTER TABLE public.defendant_response ADD CONSTRAINT defendant_response_pcs_case_id_fkey FOREIGN KEY (pcs_case_id) REFERENCES pcs_case(id);
ALTER TABLE public.defendant_response ADD CONSTRAINT defendant_response_sot_id_fkey FOREIGN KEY (sot_id) REFERENCES statement_of_truth(id);
ALTER TABLE public.defendant_response ADD CONSTRAINT defendant_response_submission_document_id_fkey FOREIGN KEY (submission_document_id) REFERENCES document(id) ON DELETE SET NULL;
ALTER TABLE public.document ADD CONSTRAINT document_case_id_fkey FOREIGN KEY (case_id) REFERENCES pcs_case(id);
ALTER TABLE public.document ADD CONSTRAINT document_claim_id_fkey FOREIGN KEY (claim_id) REFERENCES claim(id);
ALTER TABLE public.document ADD CONSTRAINT document_counter_claim_id_fkey FOREIGN KEY (counter_claim_id) REFERENCES counter_claim(id);
ALTER TABLE public.document ADD CONSTRAINT document_defendant_response_id_fkey FOREIGN KEY (defendant_response_id) REFERENCES defendant_response(id);
ALTER TABLE public.document ADD CONSTRAINT document_enf_case_id_fkey FOREIGN KEY (enf_case_id) REFERENCES enf_case(id);
ALTER TABLE public.document ADD CONSTRAINT document_general_application_id_fkey FOREIGN KEY (general_application_id) REFERENCES general_application(id);
ALTER TABLE public.document ADD CONSTRAINT document_party_id_fkey FOREIGN KEY (party_id) REFERENCES party(id);
ALTER TABLE public.enf_case ADD CONSTRAINT enf_case_claim_id_fkey FOREIGN KEY (claim_id) REFERENCES claim(id);
ALTER TABLE public.enf_case ADD CONSTRAINT enf_case_sot_id_fkey FOREIGN KEY (sot_id) REFERENCES statement_of_truth(id);
ALTER TABLE public.enf_risk_profile ADD CONSTRAINT enf_risk_profile_enf_case_id_fkey FOREIGN KEY (enf_case_id) REFERENCES enf_case(id) ON DELETE CASCADE;
ALTER TABLE public.enf_selected_defendants ADD CONSTRAINT enf_selected_defendants_enf_case_id_fkey FOREIGN KEY (enf_case_id) REFERENCES enf_case(id) ON DELETE CASCADE;
ALTER TABLE public.enf_selected_defendants ADD CONSTRAINT enf_selected_defendants_party_id_fkey FOREIGN KEY (party_id) REFERENCES party(id);
ALTER TABLE public.enf_warrant ADD CONSTRAINT enf_warrant_enf_case_id_fkey FOREIGN KEY (enf_case_id) REFERENCES enf_case(id) ON DELETE CASCADE;
ALTER TABLE public.enf_warrant_of_restitution ADD CONSTRAINT enf_warrant_of_restitution_enf_case_id_fkey FOREIGN KEY (enf_case_id) REFERENCES enf_case(id) ON DELETE CASCADE;
ALTER TABLE public.enf_writ ADD CONSTRAINT enf_writ_enf_case_id_fkey FOREIGN KEY (enf_case_id) REFERENCES enf_case(id) ON DELETE CASCADE;
ALTER TABLE public.enf_writ_of_restitution ADD CONSTRAINT enf_writ_of_restitution_enf_case_id_fkey FOREIGN KEY (enf_case_id) REFERENCES enf_case(id) ON DELETE CASCADE;
ALTER TABLE public.fee_payment ADD CONSTRAINT fk_fee_payment_claim FOREIGN KEY (possession_claim_id) REFERENCES claim(id);
ALTER TABLE public.fee_payment ADD CONSTRAINT fk_fee_payment_help_with_fees FOREIGN KEY (hwf_id) REFERENCES help_with_fees(id);
ALTER TABLE public.general_application ADD CONSTRAINT fk_general_application_party_case FOREIGN KEY (party_id, case_id) REFERENCES party(id, case_id);
ALTER TABLE public.general_application ADD CONSTRAINT general_application_case_id_fkey FOREIGN KEY (case_id) REFERENCES pcs_case(id);
ALTER TABLE public.general_application ADD CONSTRAINT general_application_hwf_id_fkey FOREIGN KEY (hwf_id) REFERENCES help_with_fees(id);
ALTER TABLE public.general_application ADD CONSTRAINT general_application_party_id_fkey FOREIGN KEY (party_id) REFERENCES party(id);
ALTER TABLE public.general_application ADD CONSTRAINT general_application_sot_id_fkey FOREIGN KEY (sot_id) REFERENCES statement_of_truth(id);
ALTER TABLE public.general_application ADD CONSTRAINT general_application_submission_document_id_fkey FOREIGN KEY (submission_document_id) REFERENCES document(id);
ALTER TABLE public.household_circumstances ADD CONSTRAINT household_circumstances_defendant_response_id_fkey FOREIGN KEY (defendant_response_id) REFERENCES defendant_response(id);
ALTER TABLE public.legal_representative ADD CONSTRAINT legal_representative_address_id_fkey FOREIGN KEY (address_id) REFERENCES address(id);
ALTER TABLE public.notice_of_possession ADD CONSTRAINT notice_of_possession_claim_id_fkey FOREIGN KEY (claim_id) REFERENCES claim(id);
ALTER TABLE public.party ADD CONSTRAINT party_address_id_fkey FOREIGN KEY (address_id) REFERENCES address(id);
ALTER TABLE public.party ADD CONSTRAINT party_case_id_fkey FOREIGN KEY (case_id) REFERENCES pcs_case(id);
ALTER TABLE public.party ADD CONSTRAINT party_contact_preferences_id_fkey FOREIGN KEY (contact_preferences_id) REFERENCES contact_preferences(id);
ALTER TABLE public.party_access_code ADD CONSTRAINT fk_party_access_code_party FOREIGN KEY (party_id, case_id) REFERENCES party(id, case_id);
ALTER TABLE public.party_access_code ADD CONSTRAINT party_access_code_case_id_fkey FOREIGN KEY (case_id) REFERENCES pcs_case(id);
ALTER TABLE public.party_attribute_assertion ADD CONSTRAINT party_attribute_assertion_created_by_fkey FOREIGN KEY (created_by) REFERENCES party(id);
ALTER TABLE public.party_attribute_assertion ADD CONSTRAINT party_attribute_assertion_evidence_document_id_fkey FOREIGN KEY (evidence_document_id) REFERENCES document(id);
ALTER TABLE public.party_attribute_assertion ADD CONSTRAINT party_attribute_assertion_last_updated_by_fkey FOREIGN KEY (last_updated_by) REFERENCES party(id);
ALTER TABLE public.party_attribute_assertion ADD CONSTRAINT party_attribute_assertion_party_id_fkey FOREIGN KEY (party_id) REFERENCES party(id);
ALTER TABLE public.payment_agreement ADD CONSTRAINT payment_agreement_defendant_response_id_fkey FOREIGN KEY (defendant_response_id) REFERENCES defendant_response(id);
ALTER TABLE public.pcs_case ADD CONSTRAINT pcs_case_property_address_id_fkey FOREIGN KEY (property_address_id) REFERENCES address(id);
ALTER TABLE public.possession_alternatives ADD CONSTRAINT possession_alternatives_claim_id_fkey FOREIGN KEY (claim_id) REFERENCES claim(id);
ALTER TABLE public.reasonable_adjustments ADD CONSTRAINT reasonable_adjustments_defendant_response_id_fkey FOREIGN KEY (defendant_response_id) REFERENCES defendant_response(id);
ALTER TABLE public.regular_expenses ADD CONSTRAINT regular_expenses_hc_id_fkey FOREIGN KEY (hc_id) REFERENCES household_circumstances(id);
ALTER TABLE public.regular_income ADD CONSTRAINT fk_regular_income_hc FOREIGN KEY (hc_id) REFERENCES household_circumstances(id) ON DELETE CASCADE;
ALTER TABLE public.regular_income_item ADD CONSTRAINT fk_income_item_regular_income FOREIGN KEY (regular_income_id) REFERENCES regular_income(id) ON DELETE CASCADE;
ALTER TABLE public.rent_arrears ADD CONSTRAINT rent_arrears_claim_id_fkey FOREIGN KEY (claim_id) REFERENCES claim(id);
ALTER TABLE public.statement_of_truth ADD CONSTRAINT statement_of_truth_claim_id_fkey FOREIGN KEY (claim_id) REFERENCES claim(id);
ALTER TABLE public.tenancy_licence ADD CONSTRAINT tenancy_licence_case_id_fkey FOREIGN KEY (case_id) REFERENCES pcs_case(id);
