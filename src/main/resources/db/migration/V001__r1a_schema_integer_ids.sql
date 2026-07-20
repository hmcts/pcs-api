-- V001__r1a_schema_integer_ids.sql
-- HDPI-7834: R1A consolidated baseline schema with integer (bigint identity) internal primary keys.
-- Source: live AAT schema (canonical), + reconciled fk_fee_payment_claim (AAT-only drift), + UUID->bigint
--   conversion of 31 internal tables. Retained UUID: pcs_case, party, claim, counter_claim,
--   general_application, enf_case, document, claim_ground, case_flag, case_party_flag, case_link_reason,
--   flag_ref_data, legal_representative. Replaces the incremental V001-V135. Pre-go-live: no data preserved.





CREATE SCHEMA draft;






CREATE TYPE public.contact_preference_type AS ENUM (
    'EMAIL',
    'POST'
);



CREATE TYPE public.income_type AS ENUM (
    'INCOME_FROM_JOBS',
    'PENSION',
    'UNIVERSAL_CREDIT',
    'OTHER_BENEFITS',
    'MONEY_FROM_ELSEWHERE'
);



CREATE TYPE public.party_attribute_assertion_status AS ENUM (
    'SUBMITTED',
    'UNDER_REVIEW',
    'ACCEPTED',
    'REJECTED'
);



CREATE TYPE public.party_attribute_assertion_submitted_by AS ENUM (
    'CLAIMANT',
    'DEFENDANT',
    'JUDGE',
    'COURT_STAFF',
    'CASE_WORKER'
);



CREATE TYPE public.recurrence_frequency AS ENUM (
    'WEEKLY',
    'MONTHLY'
);



CREATE TYPE public.statement_of_truth_completed_by AS ENUM (
    'CLAIMANT',
    'LEGAL_REPRESENTATIVE'
);



CREATE TYPE public.yes_no AS ENUM (
    'YES',
    'NO'
);



CREATE TYPE public.yes_no_na AS ENUM (
    'YES',
    'NO',
    'NOT_APPLICABLE'
);



CREATE TYPE public.yes_no_not_sure AS ENUM (
    'YES',
    'NO',
    'NOT_SURE'
);



CREATE TYPE public.yes_no_prefer_not_to_say AS ENUM (
    'YES',
    'NO',
    'PREFER_NOT_TO_SAY'
);



CREATE FUNCTION public.postcode_court_mapping_trigger_func() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
  NEW.postcode = upper(regexp_replace(NEW.postcode, '\s', '', 'g'));
  RETURN NEW;
END;
$$;





CREATE TABLE draft.draft_case_data (
    id bigint NOT NULL,
    case_reference bigint NOT NULL,
    case_data jsonb,
    event_id varchar(70) NOT NULL,
    idam_user_id uuid NOT NULL,
    party_id uuid
);



ALTER TABLE draft.draft_case_data ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME draft.draft_case_data_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);



CREATE TABLE public.address (
    id bigint NOT NULL,
    version integer,
    address_line1 varchar(100),
    address_line2 varchar(100),
    address_line3 varchar(100),
    post_town varchar(100),
    county varchar(100),
    postcode varchar(14),
    country varchar(100)
);



ALTER TABLE public.address ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.address_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);



CREATE TABLE public.asb_prohibited_conduct (
    id bigint NOT NULL,
    version integer,
    claim_id uuid,
    antisocial_behaviour public.yes_no,
    antisocial_behaviour_details varchar(500),
    illegal_purposes public.yes_no,
    illegal_purposes_details varchar(500),
    other_prohibited_conduct public.yes_no,
    other_prohibited_conduct_details varchar(500),
    claiming_standard_contract public.yes_no,
    claiming_standard_contract_details varchar(250),
    periodic_contract_agreed public.yes_no,
    periodic_contract_details varchar(250)
);



ALTER TABLE public.asb_prohibited_conduct ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.asb_prohibited_conduct_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);



CREATE TABLE public.case_flag (
    id uuid NOT NULL,
    pcs_case_id uuid NOT NULL,
    flag_ref_data_id uuid NOT NULL,
    sub_type_key varchar(50),
    sub_type_value varchar(50),
    sub_type_value_cy varchar(50),
    other_description varchar(50),
    other_description_cy varchar(50),
    flag_comment varchar(255),
    flag_comment_cy varchar(255),
    flag_update_comment varchar(255),
    date_time_created timestamp without time zone,
    date_time_modified timestamp without time zone,
    status varchar(50) NOT NULL,
    paths varchar(255) NOT NULL
);



CREATE TABLE public.case_link (
    id bigint NOT NULL,
    case_id uuid NOT NULL,
    linked_case_reference bigint NOT NULL,
    ccd_list_id varchar(50),
    created_at timestamp without time zone DEFAULT now()
);



ALTER TABLE public.case_link ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.case_link_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);



CREATE TABLE public.case_link_reason (
    id uuid NOT NULL,
    case_link_id bigint NOT NULL,
    reason_code varchar(100) NOT NULL
);



CREATE TABLE public.case_note (
    id bigint NOT NULL,
    created_on timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by varchar(50) NOT NULL,
    note varchar(500) NOT NULL,
    case_id uuid
);



ALTER TABLE public.case_note ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.case_note_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);



CREATE TABLE public.case_notification (
    id bigint NOT NULL,
    case_id uuid NOT NULL,
    provider_notification_id uuid,
    submitted_at timestamp without time zone,
    scheduled_at timestamp without time zone,
    last_updated_at timestamp without time zone NOT NULL,
    status varchar(255) NOT NULL,
    type varchar(255) NOT NULL,
    recipient varchar(255) NOT NULL,
    party_id uuid,
    claim_id uuid,
    claim_type varchar(255) NOT NULL
);



ALTER TABLE public.case_notification ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.case_notification_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);



CREATE TABLE public.case_party_flag (
    id uuid NOT NULL,
    party_id uuid,
    flag_ref_data_id uuid NOT NULL,
    sub_type_key varchar(50),
    sub_type_value varchar(50),
    sub_type_value_cy varchar(50),
    other_description varchar(50),
    other_description_cy varchar(50),
    flag_comment varchar(255),
    flag_comment_cy varchar(255),
    flag_update_comment varchar(255),
    date_time_created timestamp without time zone,
    date_time_modified timestamp without time zone,
    status varchar(50) NOT NULL,
    paths varchar(450) NOT NULL
);



CREATE TABLE public.claim (
    id uuid NOT NULL,
    version integer,
    case_id uuid,
    claimant_type text,
    against_trespassers public.yes_no,
    due_to_rent_arrears public.yes_no,
    claim_costs public.yes_no,
    pre_action_protocol_followed public.yes_no,
    mediation_attempted public.yes_no,
    settlement_attempted public.yes_no,
    claimant_circumstances_provided public.yes_no,
    claimant_circumstances varchar(950),
    additional_defendants public.yes_no,
    defendant_circumstances_provided public.yes_no,
    defendant_circumstances varchar(950),
    additional_reasons_provided public.yes_no,
    additional_reasons varchar(6400),
    underlessee_or_mortgagee public.yes_no,
    additional_underlessees_or_mortgagees public.yes_no,
    additional_docs_provided public.yes_no,
    gen_app_expected public.yes_no,
    language_used text,
    pre_action_protocol_incomplete_explanation varchar(250),
    is_exempt_landlord public.yes_no,
    claim_submitted_date timestamp with time zone,
    claim_issued_date timestamp with time zone,
    energy_performance_certificate_provided public.yes_no,
    gas_safety_report_provided public.yes_no,
    electrical_installation_condition_provided public.yes_no,
    no_energy_performance_certificate_reason varchar(500),
    no_gas_safety_report_reason varchar(500),
    no_electrical_installation_condition_reason varchar(500),
    claim_form_document_id uuid
);



CREATE TABLE public.claim_activity_log (
    id bigint NOT NULL,
    case_id uuid NOT NULL,
    party_id uuid,
    activity_type varchar NOT NULL,
    status varchar NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    details jsonb
);



ALTER TABLE public.claim_activity_log ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.claim_activity_log_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);



CREATE TABLE public.claim_document (
    claim_id uuid NOT NULL,
    document_id uuid NOT NULL
);



CREATE TABLE public.claim_ground (
    id uuid NOT NULL,
    claim_id uuid NOT NULL,
    category varchar(60) NOT NULL,
    code varchar(60) NOT NULL,
    reason varchar(500),
    description varchar(500),
    is_rent_arrears boolean NOT NULL
);



CREATE TABLE public.claim_party (
    claim_id uuid NOT NULL,
    party_id uuid NOT NULL,
    role text NOT NULL,
    rank integer NOT NULL
);



CREATE TABLE public.claim_party_legal_representative (
    party_id uuid NOT NULL,
    legal_representative_id uuid NOT NULL,
    active public.yes_no,
    start_date timestamp without time zone,
    end_date timestamp without time zone
);



CREATE TABLE public.contact_preferences (
    id bigint NOT NULL,
    contact_by_text public.yes_no,
    contact_by_phone public.yes_no,
    preference_type public.contact_preference_type,
    contact_by_email public.yes_no,
    contact_by_post public.yes_no
);



ALTER TABLE public.contact_preferences ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.contact_preferences_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);



CREATE TABLE public.counter_claim (
    id uuid NOT NULL,
    version integer,
    sot_id bigint,
    case_id uuid NOT NULL,
    party_id uuid NOT NULL,
    claim_type varchar(50),
    is_claim_amount_known public.yes_no,
    claim_amount numeric(18,2),
    estimated_max_claim_amount numeric(18,2),
    counterclaim_for varchar(6800),
    counterclaim_reasons varchar(6800),
    other_order_request_details varchar(6800),
    other_order_request_facts varchar(6800),
    need_help_with_fees public.yes_no,
    applied_for_hwf public.yes_no,
    hwf_reference_number varchar(255),
    status varchar,
    claim_submitted_date timestamp without time zone,
    claim_issued_date timestamp without time zone,
    last_modified_date timestamp without time zone,
    language_used text
);



CREATE TABLE public.counter_claim_party (
    id bigint NOT NULL,
    cc_id uuid NOT NULL,
    party_id uuid NOT NULL
);



ALTER TABLE public.counter_claim_party ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.counter_claim_party_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);



CREATE TABLE public.defendant_response (
    id bigint NOT NULL,
    claim_id uuid NOT NULL,
    party_id uuid NOT NULL,
    free_legal_advice public.yes_no_prefer_not_to_say,
    tenancy_start_date_confirmation public.yes_no_not_sure,
    tenancy_start_date date,
    landlord_registered public.yes_no_not_sure,
    defendant_name_confirmation public.yes_no,
    sot_id bigint,
    pcs_case_id uuid NOT NULL,
    correspondence_address_confirmation public.yes_no,
    possession_notice_received public.yes_no_not_sure,
    notice_received_date date,
    rent_arrears_amount_confirmation public.yes_no_not_sure,
    dispute_claim public.yes_no,
    dispute_claim_details varchar(6800),
    make_counter_claim public.yes_no,
    version integer,
    status varchar(60),
    response_submitted_date timestamp without time zone,
    response_deleted_date timestamp without time zone,
    response_received_date timestamp without time zone,
    language_used text,
    channel varchar(60),
    ingestion_source varchar(60),
    landlord_licensed public.yes_no_not_sure,
    written_terms public.yes_no_not_sure,
    other_considerations public.yes_no,
    other_considerations_details varchar(6400),
    tenancy_type_confirmation public.yes_no_not_sure,
    counter_claim_want_to_upload_files public.yes_no,
    submission_document_id uuid
);



ALTER TABLE public.defendant_response ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.defendant_response_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);



CREATE TABLE public.document (
    id uuid NOT NULL,
    case_id uuid,
    url text,
    file_name text,
    binary_url text,
    category_id text DEFAULT 'uncategorisedDocuments'::text NOT NULL,
    type text,
    description varchar(60),
    enf_case_id uuid,
    counter_claim_id uuid,
    content_type varchar(200),
    size bigint,
    display_file_name text,
    claim_id uuid,
    defendant_response_id bigint,
    party_id uuid,
    general_application_id uuid,
    submitted_date timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    document_id uuid
);



CREATE TABLE public.eligibility_whitelisted_epim (
    epims_id integer NOT NULL,
    eligible_from date NOT NULL,
    audit jsonb NOT NULL
);



CREATE TABLE public.enf_case (
    id uuid NOT NULL,
    claim_id uuid NOT NULL,
    enforcement_order jsonb NOT NULL,
    bailiff_date timestamp without time zone,
    sot_id bigint
);



CREATE TABLE public.enf_risk_profile (
    id bigint NOT NULL,
    enf_case_id uuid NOT NULL,
    any_risk_to_bailiff public.yes_no_not_sure,
    vulnerable_people_present public.yes_no_not_sure,
    vulnerable_category varchar(100),
    vulnerable_reason_text varchar(6800),
    violent_details varchar(6800),
    firearms_details varchar(6800),
    criminal_details varchar(6800),
    verbal_threats_details varchar(6800),
    protest_group_details varchar(6800),
    police_social_services_details varchar(6800),
    animals_details varchar(6800)
);



ALTER TABLE public.enf_risk_profile ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.enf_risk_profile_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);



CREATE TABLE public.enf_selected_defendants (
    id bigint NOT NULL,
    enf_case_id uuid NOT NULL,
    party_id uuid NOT NULL
);



ALTER TABLE public.enf_selected_defendants ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.enf_selected_defendants_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);



CREATE TABLE public.enf_warrant (
    id bigint NOT NULL,
    enf_case_id uuid NOT NULL,
    show_people_who_will_be_evicted_page public.yes_no,
    show_people_you_want_to_evict_page public.yes_no,
    is_suspended_order public.yes_no,
    additional_information_select public.yes_no,
    additional_information_details varchar(6800),
    correct_name_and_address public.yes_no,
    evict_everyone public.yes_no,
    is_difficult_to_access_property public.yes_no,
    clarification_on_access_difficulty_text varchar(6800),
    are_legal_costs_to_be_claimed public.yes_no,
    amount_of_legal_costs numeric(10,2),
    amount_owed numeric(10,2),
    have_land_registry_fees_been_paid public.yes_no,
    amount_of_land_registry_fees numeric(10,2),
    repayment_choice varchar(20),
    amount_of_repayment_costs numeric(10,2),
    repayment_summary_markdown text,
    defendants_dob_known public.yes_no,
    defendants_dob_details varchar(6800),
    completed_by public.statement_of_truth_completed_by,
    certification text,
    agreement_claimant text,
    full_name_claimant varchar(100),
    position_claimant varchar(100),
    agreement_legal_rep text,
    full_name_legal_rep varchar(100),
    firm_name_legal_rep varchar(100),
    position_legal_rep varchar(100),
    created timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    language_used varchar(30)
);



ALTER TABLE public.enf_warrant ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.enf_warrant_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);



CREATE TABLE public.enf_warrant_of_restitution (
    id bigint NOT NULL,
    enf_case_id uuid NOT NULL,
    created timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    language_used varchar(30),
    how_defendants_returned varchar(6800),
    is_difficult_to_access_property public.yes_no,
    clarification_on_access_difficulty_text varchar(6800),
    additional_information_select public.yes_no,
    additional_information_details varchar(6800)
);



ALTER TABLE public.enf_warrant_of_restitution ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.enf_warrant_of_restitution_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);



CREATE TABLE public.enf_writ (
    id bigint NOT NULL,
    enf_case_id uuid NOT NULL,
    correct_name_and_address public.yes_no,
    has_hired_high_court_enforcement_officer public.yes_no,
    hceo_details varchar(120),
    has_claim_transferred_to_high_court public.yes_no,
    have_land_registry_fees_been_paid public.yes_no,
    amount_of_land_registry_fees numeric(10,2),
    are_legal_costs_to_be_claimed public.yes_no,
    amount_of_legal_costs numeric(10,2),
    amount_owed numeric(10,2),
    created timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    language_used varchar(30),
    repayment_choice varchar(20),
    amount_of_repayment_costs numeric(10,2),
    repayment_summary_markdown text
);



ALTER TABLE public.enf_writ ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.enf_writ_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);



CREATE TABLE public.enf_writ_of_restitution (
    id bigint NOT NULL,
    enf_case_id uuid NOT NULL,
    created timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    language_used varchar(30)
);



ALTER TABLE public.enf_writ_of_restitution ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.enf_writ_of_restitution_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);



CREATE TABLE public.fee_payment (
    id bigint NOT NULL,
    party_id uuid,
    request_date timestamp without time zone NOT NULL,
    service_request_reference varchar(255),
    external_reference varchar(255),
    amount numeric(19,2),
    hwf_id bigint,
    payment_callback_handler_type varchar(30) NOT NULL,
    task_data jsonb,
    status varchar(50),
    possession_claim_id uuid,
    related_entity_id uuid
);



ALTER TABLE public.fee_payment ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.fee_payment_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);



CREATE TABLE public.flag_ref_data (
    id uuid NOT NULL,
    flag_code varchar(10),
    name varchar(255),
    name_cy varchar(255),
    hearing_relevant boolean,
    available_externally boolean,
    visibility varchar(20)
);



CREATE TABLE public.general_application (
    id uuid NOT NULL,
    case_id uuid NOT NULL,
    sot_id bigint,
    hwf_id bigint,
    type varchar(50) NOT NULL,
    state varchar(30),
    party_id uuid NOT NULL,
    within_14_days public.yes_no,
    need_hwf public.yes_no,
    applied_for_hwf public.yes_no,
    other_parties_agreed public.yes_no,
    without_notice public.yes_no,
    without_notice_reason varchar(6800),
    what_order_wanted varchar(6800),
    documents_uploaded public.yes_no,
    language_used varchar(30),
    application_submitted_date timestamp without time zone,
    application_issued_date timestamp without time zone,
    client_reference varchar(60),
    rank integer NOT NULL,
    submission_document_id uuid
);



CREATE TABLE public.help_with_fees (
    id bigint NOT NULL,
    hwf_reference varchar(60) NOT NULL
);



ALTER TABLE public.help_with_fees ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.help_with_fees_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);



CREATE TABLE public.household_circumstances (
    id bigint NOT NULL,
    defendant_response_id bigint NOT NULL,
    dependant_children public.yes_no,
    dependant_children_details varchar(500),
    other_dependants public.yes_no,
    other_dependant_details varchar(500),
    other_tenants public.yes_no,
    other_tenants_details varchar(500),
    alternative_accommodation public.yes_no_not_sure,
    alternative_accommodation_transfer_date date,
    share_additional_circumstances public.yes_no,
    additional_circumstances_details varchar(500),
    exceptional_hardship public.yes_no,
    exceptional_hardship_details varchar(500),
    share_income_expense_details public.yes_no,
    universal_credit public.yes_no,
    uc_application_date date,
    priority_debts public.yes_no,
    debt_total numeric(18,2),
    debt_contribution numeric(18,2),
    debt_contribution_frequency varchar(60),
    regular_expenses varchar(500),
    expense_amount numeric(18,2),
    expense_frequency varchar(60)
);



ALTER TABLE public.household_circumstances ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.household_circumstances_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);



CREATE TABLE public.legal_representative (
    id uuid NOT NULL,
    idam_id uuid,
    organisation_name varchar(120),
    first_name varchar(60),
    last_name varchar(60),
    email varchar(120),
    phone varchar(40),
    address_id bigint,
    organisation_id varchar(64)
);



CREATE TABLE public.notice_of_possession (
    id bigint NOT NULL,
    version integer,
    claim_id uuid,
    notice_served public.yes_no NOT NULL,
    notice_type varchar(60),
    serving_method varchar(40),
    notice_details varchar(250),
    notice_date date,
    notice_date_time timestamp without time zone,
    notice_statement varchar(500),
    unable_to_upload_reason varchar(500),
    is_able_to_upload_document public.yes_no,
    CONSTRAINT chk_notice_date CHECK (((notice_date IS NULL) OR (notice_date_time IS NULL)))
);



ALTER TABLE public.notice_of_possession ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.notice_of_possession_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);



CREATE TABLE public.party (
    id uuid NOT NULL,
    version integer,
    case_id uuid,
    type text,
    idam_id uuid,
    first_name varchar(60),
    last_name varchar(60),
    org_name varchar(60),
    name_known public.yes_no,
    name_overridden public.yes_no,
    address_id bigint,
    address_known public.yes_no,
    address_same_as_property public.yes_no,
    phone_number_provided public.yes_no,
    phone_number varchar(60),
    email_address varchar(60),
    pcq_id varchar(60),
    contact_preferences_id bigint,
    dob date,
    organisation_id varchar(64)
);



CREATE TABLE public.party_access_code (
    id bigint NOT NULL,
    case_id uuid NOT NULL,
    party_id uuid NOT NULL,
    code varchar(100) NOT NULL,
    role varchar(20),
    created timestamp without time zone
);



ALTER TABLE public.party_access_code ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.party_access_code_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);



CREATE TABLE public.party_attribute_assertion (
    id bigint NOT NULL,
    party_id uuid NOT NULL,
    evidence_document_id uuid,
    attributes_name varchar(255) NOT NULL,
    asserted_value text NOT NULL,
    asserted_by public.party_attribute_assertion_submitted_by NOT NULL,
    status public.party_attribute_assertion_status NOT NULL,
    created_at timestamp without time zone NOT NULL,
    last_updated_at timestamp without time zone,
    created_by uuid NOT NULL,
    last_updated_by uuid NOT NULL
);



ALTER TABLE public.party_attribute_assertion ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.party_attribute_assertion_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);



CREATE TABLE public.payment_agreement (
    id bigint NOT NULL,
    defendant_response_id bigint NOT NULL,
    any_payments_made public.yes_no,
    payment_details varchar(500),
    paid_money_to_housing_org public.yes_no,
    repayment_plan_agreed public.yes_no_not_sure,
    repayment_agreed_details varchar(500),
    repay_arrears_instalments public.yes_no,
    additional_rent_contribution numeric(18,2),
    additional_contribution_frequency varchar(50)
);



ALTER TABLE public.payment_agreement ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.payment_agreement_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);



CREATE TABLE public.pcs_case (
    id uuid NOT NULL,
    version integer,
    case_reference bigint,
    property_address_id bigint,
    case_management_location integer,
    pre_action_protocol_completed boolean,
    legislative_country varchar(20),
    party_documents jsonb,
    claimant_type varchar(50),
    region_id integer,
    base_location integer
);



CREATE TABLE public.possession_alternatives (
    id bigint NOT NULL,
    version integer,
    claim_id uuid,
    supension_of_rtb_requested public.yes_no NOT NULL,
    supension_of_rtb_housing_act_section varchar(20),
    supension_of_rtb_reason varchar(250),
    dot_requested public.yes_no NOT NULL,
    dot_housing_act_section varchar(20),
    dot_statement_served public.yes_no,
    dot_statement_details varchar(950),
    dot_reason varchar(250)
);



ALTER TABLE public.possession_alternatives ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.possession_alternatives_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);



CREATE TABLE public.postcode_court_mapping (
    postcode varchar(20) NOT NULL,
    epims_id integer NOT NULL,
    legislative_country varchar(80) NOT NULL,
    effective_from date NOT NULL,
    effective_to date,
    audit jsonb NOT NULL
);



CREATE TABLE public.reasonable_adjustments (
    id bigint NOT NULL,
    defendant_response_id bigint NOT NULL,
    reasonable_adjustments_required varchar(250),
    reasonable_adjustment_description varchar(500),
    hearing_enhancement_description varchar(250),
    sign_language_support_description varchar(250),
    travel_support_description varchar(250),
    welsh_language_requirements varchar(250),
    language_interpreter public.yes_no,
    language_support_description varchar(250),
    considered_vulnerable public.yes_no,
    vulnerable_characteristic_description varchar(250)
);



ALTER TABLE public.reasonable_adjustments ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.reasonable_adjustments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);



CREATE TABLE public.regular_expenses (
    id bigint NOT NULL,
    hc_id bigint NOT NULL,
    expense_type varchar(30) NOT NULL,
    amount numeric(18,2) NOT NULL,
    expense_frequency varchar(10) NOT NULL,
    CONSTRAINT chk_regular_expense_amount_positive CHECK ((amount >= (0)::numeric))
);



ALTER TABLE public.regular_expenses ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.regular_expenses_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);



CREATE TABLE public.regular_income (
    id bigint NOT NULL,
    hc_id bigint NOT NULL,
    other_income_details varchar(500),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);



ALTER TABLE public.regular_income ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.regular_income_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);



CREATE TABLE public.regular_income_item (
    id bigint NOT NULL,
    regular_income_id bigint NOT NULL,
    income_type public.income_type NOT NULL,
    amount numeric(18,2),
    frequency public.recurrence_frequency,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_income_item_amount CHECK (((amount IS NULL) OR (amount >= (0)::numeric)))
);



ALTER TABLE public.regular_income_item ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.regular_income_item_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);



CREATE TABLE public.rent_arrears (
    id bigint NOT NULL,
    version integer,
    claim_id uuid,
    total_rent_arrears numeric(18,2) NOT NULL,
    arrears_judgment_wanted public.yes_no,
    recovery_attempted public.yes_no,
    recovery_attempt_details varchar(500)
);



ALTER TABLE public.rent_arrears ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.rent_arrears_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);



CREATE TABLE public.scheduled_tasks (
    task_name text NOT NULL,
    task_instance text NOT NULL,
    task_data bytea,
    execution_time timestamp with time zone NOT NULL,
    picked boolean NOT NULL,
    picked_by text,
    last_success timestamp with time zone,
    last_failure timestamp with time zone,
    consecutive_failures integer,
    last_heartbeat timestamp with time zone,
    version bigint NOT NULL,
    priority smallint
);



CREATE TABLE public.statement_of_truth (
    id bigint NOT NULL,
    version integer,
    claim_id uuid,
    completed_by varchar(40),
    accepted public.yes_no NOT NULL,
    full_name varchar(100) NOT NULL,
    firm_name varchar(100),
    position_held varchar(100),
    completed_date timestamp without time zone
);



ALTER TABLE public.statement_of_truth ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.statement_of_truth_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);



CREATE TABLE public.tenancy_licence (
    id bigint NOT NULL,
    version integer,
    case_id uuid,
    type varchar(40) NOT NULL,
    other_type_details varchar(500),
    start_date date,
    rent_amount numeric(18,2),
    rent_frequency varchar(20),
    other_rent_frequency varchar(60),
    rent_per_day numeric(18,2),
    calculated_daily_rent_correct public.yes_no,
    has_copy_of_tenancy_licence public.yes_no,
    reasons_for_no_tenancy_licence varchar(500)
);



ALTER TABLE public.tenancy_licence ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tenancy_licence_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);



ALTER TABLE ONLY draft.draft_case_data
    ADD CONSTRAINT unsubmitted_case_data_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.address
    ADD CONSTRAINT address_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.asb_prohibited_conduct
    ADD CONSTRAINT asb_prohibited_conduct_claim_id_key UNIQUE (claim_id);



ALTER TABLE ONLY public.asb_prohibited_conduct
    ADD CONSTRAINT asb_prohibited_conduct_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.case_flag
    ADD CONSTRAINT case_flag_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.case_link
    ADD CONSTRAINT case_link_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.case_link_reason
    ADD CONSTRAINT case_link_reason_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.case_note
    ADD CONSTRAINT case_note_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.case_party_flag
    ADD CONSTRAINT case_party_flag_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.claim_activity_log
    ADD CONSTRAINT claim_activity_log_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.claim_document
    ADD CONSTRAINT claim_document_pkey PRIMARY KEY (claim_id, document_id);



ALTER TABLE ONLY public.claim_ground
    ADD CONSTRAINT claim_ground_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.claim_party_legal_representative
    ADD CONSTRAINT claim_party_legal_representative_pkey PRIMARY KEY (party_id, legal_representative_id);



ALTER TABLE ONLY public.claim_party
    ADD CONSTRAINT claim_party_pkey PRIMARY KEY (claim_id, party_id);



ALTER TABLE ONLY public.claim
    ADD CONSTRAINT claim_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.contact_preferences
    ADD CONSTRAINT contact_preferences_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.counter_claim_party
    ADD CONSTRAINT counter_claim_party_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.counter_claim
    ADD CONSTRAINT counter_claim_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.defendant_response
    ADD CONSTRAINT defendant_response_claim_party UNIQUE (claim_id, party_id);



ALTER TABLE ONLY public.defendant_response
    ADD CONSTRAINT defendant_response_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.defendant_response
    ADD CONSTRAINT defendant_response_sot_id_key UNIQUE (sot_id);



ALTER TABLE ONLY public.document
    ADD CONSTRAINT document_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.eligibility_whitelisted_epim
    ADD CONSTRAINT eligibility_whitelisted_epim_pkey PRIMARY KEY (epims_id);



ALTER TABLE ONLY public.enf_case
    ADD CONSTRAINT enf_case_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.enf_risk_profile
    ADD CONSTRAINT enf_risk_profile_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.enf_selected_defendants
    ADD CONSTRAINT enf_selected_defendants_enf_case_id_party_id_key UNIQUE (enf_case_id, party_id);



ALTER TABLE ONLY public.enf_selected_defendants
    ADD CONSTRAINT enf_selected_defendants_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.enf_warrant_of_restitution
    ADD CONSTRAINT enf_warrant_of_restitution_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.enf_warrant
    ADD CONSTRAINT enf_warrant_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.enf_writ_of_restitution
    ADD CONSTRAINT enf_writ_of_restitution_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.enf_writ
    ADD CONSTRAINT enf_writ_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.flag_ref_data
    ADD CONSTRAINT flag_ref_data_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.general_application
    ADD CONSTRAINT general_application_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.help_with_fees
    ADD CONSTRAINT help_with_fees_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.household_circumstances
    ADD CONSTRAINT household_circumstances_defendant_response_id_key UNIQUE (defendant_response_id);



ALTER TABLE ONLY public.household_circumstances
    ADD CONSTRAINT household_circumstances_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.legal_representative
    ADD CONSTRAINT legal_representative_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.notice_of_possession
    ADD CONSTRAINT notice_of_possession_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.party_access_code
    ADD CONSTRAINT party_access_code_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.party_attribute_assertion
    ADD CONSTRAINT party_attribute_assertion_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.party
    ADD CONSTRAINT party_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.payment_agreement
    ADD CONSTRAINT payment_agreement_defendant_response_id_key UNIQUE (defendant_response_id);



ALTER TABLE ONLY public.payment_agreement
    ADD CONSTRAINT payment_agreement_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.pcs_case
    ADD CONSTRAINT pcs_case_case_reference_key UNIQUE (case_reference);



ALTER TABLE ONLY public.pcs_case
    ADD CONSTRAINT pcs_case_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.case_notification
    ADD CONSTRAINT pk_case_notification_id PRIMARY KEY (id);



ALTER TABLE ONLY public.fee_payment
    ADD CONSTRAINT pk_fee_payment PRIMARY KEY (id);



ALTER TABLE ONLY public.possession_alternatives
    ADD CONSTRAINT possession_alternatives_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.postcode_court_mapping
    ADD CONSTRAINT postcode_court_mapping_pkey PRIMARY KEY (postcode, epims_id);



ALTER TABLE ONLY public.reasonable_adjustments
    ADD CONSTRAINT reasonable_adjustments_defendant_response_id_key UNIQUE (defendant_response_id);



ALTER TABLE ONLY public.reasonable_adjustments
    ADD CONSTRAINT reasonable_adjustments_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.regular_expenses
    ADD CONSTRAINT regular_expenses_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.regular_income_item
    ADD CONSTRAINT regular_income_item_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.regular_income
    ADD CONSTRAINT regular_income_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.rent_arrears
    ADD CONSTRAINT rent_arrears_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.scheduled_tasks
    ADD CONSTRAINT scheduled_tasks_pkey PRIMARY KEY (task_name, task_instance);



ALTER TABLE ONLY public.statement_of_truth
    ADD CONSTRAINT statement_of_truth_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.tenancy_licence
    ADD CONSTRAINT tenancy_licence_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.enf_risk_profile
    ADD CONSTRAINT unique_risk_profile_per_case UNIQUE (enf_case_id);



ALTER TABLE ONLY public.fee_payment
    ADD CONSTRAINT unique_service_request_ref UNIQUE (service_request_reference);



ALTER TABLE ONLY public.enf_warrant_of_restitution
    ADD CONSTRAINT unique_warrant_of_restitution_per_enforcement UNIQUE (enf_case_id);



ALTER TABLE ONLY public.enf_warrant
    ADD CONSTRAINT unique_warrant_per_enforcement UNIQUE (enf_case_id);



ALTER TABLE ONLY public.enf_writ_of_restitution
    ADD CONSTRAINT unique_writ_of_restitution_per_enforcement UNIQUE (enf_case_id);



ALTER TABLE ONLY public.enf_writ
    ADD CONSTRAINT unique_writ_per_enforcement UNIQUE (enf_case_id);



ALTER TABLE ONLY public.claim_party
    ADD CONSTRAINT uq_claim_role_rank UNIQUE (claim_id, role, rank);



ALTER TABLE ONLY public.general_application
    ADD CONSTRAINT uq_general_application_case_rank UNIQUE (case_id, rank);



ALTER TABLE ONLY public.regular_income_item
    ADD CONSTRAINT uq_income_item_type UNIQUE (regular_income_id, income_type);



ALTER TABLE ONLY public.party_access_code
    ADD CONSTRAINT uq_party_access_code_case_code UNIQUE (case_id, code);



ALTER TABLE ONLY public.party_access_code
    ADD CONSTRAINT uq_party_access_code_case_party UNIQUE (case_id, party_id);



ALTER TABLE ONLY public.party
    ADD CONSTRAINT uq_party_id_case UNIQUE (id, case_id);



ALTER TABLE ONLY public.regular_income
    ADD CONSTRAINT uq_regular_income_hc UNIQUE (hc_id);



CREATE INDEX draft_case_data_case_ref_idx ON draft.draft_case_data USING btree (case_reference);



CREATE INDEX draft_case_data_case_reference_idx ON draft.draft_case_data USING btree (case_reference);



CREATE INDEX draft_case_data_party_id_idx ON draft.draft_case_data USING btree (party_id);



CREATE UNIQUE INDEX draft_case_data_unique_idx ON draft.draft_case_data USING btree (case_reference, event_id, idam_user_id, party_id);



CREATE INDEX draft_case_data_user_id_idx ON draft.draft_case_data USING btree (idam_user_id);



CREATE INDEX defendant_response_pcs_case_idx ON public.defendant_response USING btree (pcs_case_id);



CREATE INDEX execution_time_idx ON public.scheduled_tasks USING btree (execution_time);



CREATE UNIQUE INDEX general_application_case_id_client_ref ON public.general_application USING btree (case_id, client_reference);



CREATE INDEX idx_case_flag_pcs_case_id ON public.case_flag USING btree (pcs_case_id);



CREATE INDEX idx_case_link_reason_link ON public.case_link_reason USING btree (case_link_id);



CREATE INDEX idx_case_note_case_id ON public.case_note USING btree (case_id);



CREATE INDEX idx_case_party_flag_party_id ON public.case_party_flag USING btree (party_id);



CREATE INDEX idx_claim_activity_log_case_id ON public.claim_activity_log USING btree (case_id);



CREATE INDEX idx_claim_activity_log_party_id ON public.claim_activity_log USING btree (party_id);



CREATE INDEX idx_claim_activity_log_type_status ON public.claim_activity_log USING btree (activity_type, status);



CREATE INDEX idx_claim_case_id ON public.claim USING btree (case_id);



CREATE INDEX idx_claim_claim_form_document_id ON public.claim USING btree (claim_form_document_id);



CREATE INDEX idx_claim_document_claim_id ON public.claim_document USING btree (claim_id);



CREATE INDEX idx_claim_document_document_id ON public.claim_document USING btree (document_id);



CREATE INDEX idx_claim_ground_claim_id ON public.claim_ground USING btree (claim_id);



CREATE INDEX idx_counter_claim_party_party_id ON public.counter_claim_party USING btree (party_id);



CREATE INDEX idx_defendant_response_submission_document_id ON public.defendant_response USING btree (submission_document_id);



CREATE INDEX idx_document_case_id ON public.document USING btree (case_id);



CREATE INDEX idx_document_claim_id ON public.document USING btree (claim_id);



CREATE INDEX idx_document_counter_claim_id ON public.document USING btree (counter_claim_id);



CREATE INDEX idx_document_defendant_response_id ON public.document USING btree (defendant_response_id);



CREATE INDEX idx_document_enf_case_id ON public.document USING btree (enf_case_id);



CREATE INDEX idx_document_general_application_id ON public.document USING btree (general_application_id);



CREATE INDEX idx_document_party_id ON public.document USING btree (party_id);



CREATE INDEX idx_eligibility_whitelisted_epim_eligible_from ON public.eligibility_whitelisted_epim USING btree (eligible_from);



CREATE INDEX idx_enf_case_claim_id ON public.enf_case USING btree (claim_id);



CREATE INDEX idx_enf_case_sot_id ON public.enf_case USING btree (sot_id);



CREATE INDEX idx_enf_risk_profile_case_id ON public.enf_risk_profile USING btree (enf_case_id);



CREATE INDEX idx_enf_selected_defendants_party ON public.enf_selected_defendants USING btree (party_id);



CREATE INDEX idx_fee_payment_possession_claim_id ON public.fee_payment USING btree (possession_claim_id);



CREATE INDEX idx_fee_payment_request_reference ON public.fee_payment USING btree (service_request_reference);



CREATE INDEX idx_idam_id ON public.party USING btree (idam_id);



CREATE INDEX idx_income_item_regular_income_id ON public.regular_income_item USING btree (regular_income_id);



CREATE INDEX idx_notice_of_possession_claim_id ON public.notice_of_possession USING btree (claim_id);



CREATE INDEX idx_party_access_code_case_code ON public.party_access_code USING btree (case_id, code);



CREATE INDEX idx_party_attribute_assertion_party_id ON public.party_attribute_assertion USING btree (party_id);



CREATE INDEX idx_party_case_id ON public.party USING btree (case_id);



CREATE INDEX idx_party_id ON public.claim_party USING btree (party_id);



CREATE INDEX idx_pcs_case_party_documents ON public.pcs_case USING gin (party_documents);



CREATE INDEX idx_possession_alternatives_claim_id ON public.possession_alternatives USING btree (claim_id);



CREATE INDEX idx_postcode ON public.postcode_court_mapping USING btree (postcode);



CREATE INDEX idx_regular_income_hc_id ON public.regular_income USING btree (hc_id);



CREATE INDEX idx_rent_arrears_claim_id ON public.rent_arrears USING btree (claim_id);



CREATE INDEX idx_statement_of_truth_claim_id ON public.statement_of_truth USING btree (claim_id);



CREATE INDEX idx_tenancy_licence_case_id ON public.tenancy_licence USING btree (case_id);



CREATE INDEX last_heartbeat_idx ON public.scheduled_tasks USING btree (last_heartbeat);



CREATE INDEX priority_execution_time_idx ON public.scheduled_tasks USING btree (priority DESC, execution_time);



CREATE UNIQUE INDEX ux_case_link_unique ON public.case_link USING btree (case_id, linked_case_reference);



CREATE UNIQUE INDEX ux_counter_claim_party ON public.counter_claim_party USING btree (cc_id, party_id);



CREATE TRIGGER postcode_court_mapping_trigger BEFORE INSERT OR UPDATE ON public.postcode_court_mapping FOR EACH ROW EXECUTE FUNCTION public.postcode_court_mapping_trigger_func();



ALTER TABLE ONLY public.asb_prohibited_conduct
    ADD CONSTRAINT asb_prohibited_conduct_claim_id_fkey FOREIGN KEY (claim_id) REFERENCES public.claim(id);



ALTER TABLE ONLY public.case_flag
    ADD CONSTRAINT case_flag_flag_ref_data_id_fkey FOREIGN KEY (flag_ref_data_id) REFERENCES public.flag_ref_data(id) ON DELETE CASCADE;



ALTER TABLE ONLY public.case_flag
    ADD CONSTRAINT case_flag_pcs_case_id_fkey FOREIGN KEY (pcs_case_id) REFERENCES public.pcs_case(id) ON DELETE CASCADE;



ALTER TABLE ONLY public.case_link
    ADD CONSTRAINT case_link_case_id_fkey FOREIGN KEY (case_id) REFERENCES public.pcs_case(id) ON DELETE CASCADE;



ALTER TABLE ONLY public.case_link_reason
    ADD CONSTRAINT case_link_reason_case_link_id_fkey FOREIGN KEY (case_link_id) REFERENCES public.case_link(id) ON DELETE CASCADE;



ALTER TABLE ONLY public.case_note
    ADD CONSTRAINT case_note_case_id_fkey FOREIGN KEY (case_id) REFERENCES public.pcs_case(id);



ALTER TABLE ONLY public.case_party_flag
    ADD CONSTRAINT case_party_flag_flag_ref_data_id_fkey FOREIGN KEY (flag_ref_data_id) REFERENCES public.flag_ref_data(id) ON DELETE CASCADE;



ALTER TABLE ONLY public.case_party_flag
    ADD CONSTRAINT case_party_flag_party_id_fkey FOREIGN KEY (party_id) REFERENCES public.party(id) ON DELETE CASCADE;



ALTER TABLE ONLY public.claim_activity_log
    ADD CONSTRAINT claim_activity_log_case_id_fkey FOREIGN KEY (case_id) REFERENCES public.pcs_case(id);



ALTER TABLE ONLY public.claim_activity_log
    ADD CONSTRAINT claim_activity_log_party_id_fkey FOREIGN KEY (party_id) REFERENCES public.party(id);



ALTER TABLE ONLY public.claim
    ADD CONSTRAINT claim_case_id_fkey FOREIGN KEY (case_id) REFERENCES public.pcs_case(id);



ALTER TABLE ONLY public.claim
    ADD CONSTRAINT claim_claim_form_document_id_fkey FOREIGN KEY (claim_form_document_id) REFERENCES public.document(id);



ALTER TABLE ONLY public.claim_document
    ADD CONSTRAINT claim_document_claim_id_fkey FOREIGN KEY (claim_id) REFERENCES public.claim(id);



ALTER TABLE ONLY public.claim_document
    ADD CONSTRAINT claim_document_document_id_fkey FOREIGN KEY (document_id) REFERENCES public.document(id);



ALTER TABLE ONLY public.claim_ground
    ADD CONSTRAINT claim_ground_claim_id_fkey FOREIGN KEY (claim_id) REFERENCES public.claim(id);



ALTER TABLE ONLY public.claim_party
    ADD CONSTRAINT claim_party_claim_id_fkey FOREIGN KEY (claim_id) REFERENCES public.claim(id);



ALTER TABLE ONLY public.claim_party_legal_representative
    ADD CONSTRAINT claim_party_legal_representative_legal_representative_id_fkey FOREIGN KEY (legal_representative_id) REFERENCES public.legal_representative(id);



ALTER TABLE ONLY public.claim_party_legal_representative
    ADD CONSTRAINT claim_party_legal_representative_party_id_fkey FOREIGN KEY (party_id) REFERENCES public.party(id);



ALTER TABLE ONLY public.claim_party
    ADD CONSTRAINT claim_party_party_id_fkey FOREIGN KEY (party_id) REFERENCES public.party(id);



ALTER TABLE ONLY public.counter_claim
    ADD CONSTRAINT counter_claim_case_id_fkey FOREIGN KEY (case_id) REFERENCES public.pcs_case(id);



ALTER TABLE ONLY public.counter_claim_party
    ADD CONSTRAINT counter_claim_party_cc_id_fkey FOREIGN KEY (cc_id) REFERENCES public.counter_claim(id);



ALTER TABLE ONLY public.counter_claim
    ADD CONSTRAINT counter_claim_party_id_fkey FOREIGN KEY (party_id) REFERENCES public.party(id);



ALTER TABLE ONLY public.counter_claim_party
    ADD CONSTRAINT counter_claim_party_party_id_fkey FOREIGN KEY (party_id) REFERENCES public.party(id);



ALTER TABLE ONLY public.counter_claim
    ADD CONSTRAINT counter_claim_sot_id_fkey FOREIGN KEY (sot_id) REFERENCES public.statement_of_truth(id);



ALTER TABLE ONLY public.defendant_response
    ADD CONSTRAINT defendant_response_claim_id_fkey FOREIGN KEY (claim_id) REFERENCES public.claim(id);



ALTER TABLE ONLY public.defendant_response
    ADD CONSTRAINT defendant_response_party_id_fkey FOREIGN KEY (party_id) REFERENCES public.party(id);



ALTER TABLE ONLY public.defendant_response
    ADD CONSTRAINT defendant_response_pcs_case_id_fkey FOREIGN KEY (pcs_case_id) REFERENCES public.pcs_case(id);



ALTER TABLE ONLY public.defendant_response
    ADD CONSTRAINT defendant_response_sot_id_fkey FOREIGN KEY (sot_id) REFERENCES public.statement_of_truth(id);



ALTER TABLE ONLY public.defendant_response
    ADD CONSTRAINT defendant_response_submission_document_id_fkey FOREIGN KEY (submission_document_id) REFERENCES public.document(id) ON DELETE SET NULL;



ALTER TABLE ONLY public.document
    ADD CONSTRAINT document_case_id_fkey FOREIGN KEY (case_id) REFERENCES public.pcs_case(id);



ALTER TABLE ONLY public.document
    ADD CONSTRAINT document_claim_id_fkey FOREIGN KEY (claim_id) REFERENCES public.claim(id);



ALTER TABLE ONLY public.document
    ADD CONSTRAINT document_counter_claim_id_fkey FOREIGN KEY (counter_claim_id) REFERENCES public.counter_claim(id);



ALTER TABLE ONLY public.document
    ADD CONSTRAINT document_defendant_response_id_fkey FOREIGN KEY (defendant_response_id) REFERENCES public.defendant_response(id);



ALTER TABLE ONLY public.document
    ADD CONSTRAINT document_enf_case_id_fkey FOREIGN KEY (enf_case_id) REFERENCES public.enf_case(id);



ALTER TABLE ONLY public.document
    ADD CONSTRAINT document_general_application_id_fkey FOREIGN KEY (general_application_id) REFERENCES public.general_application(id);



ALTER TABLE ONLY public.document
    ADD CONSTRAINT document_party_id_fkey FOREIGN KEY (party_id) REFERENCES public.party(id);



ALTER TABLE ONLY public.enf_case
    ADD CONSTRAINT enf_case_claim_id_fkey FOREIGN KEY (claim_id) REFERENCES public.claim(id);



ALTER TABLE ONLY public.enf_case
    ADD CONSTRAINT enf_case_sot_id_fkey FOREIGN KEY (sot_id) REFERENCES public.statement_of_truth(id);



ALTER TABLE ONLY public.enf_risk_profile
    ADD CONSTRAINT enf_risk_profile_enf_case_id_fkey FOREIGN KEY (enf_case_id) REFERENCES public.enf_case(id) ON DELETE CASCADE;



ALTER TABLE ONLY public.enf_selected_defendants
    ADD CONSTRAINT enf_selected_defendants_enf_case_id_fkey FOREIGN KEY (enf_case_id) REFERENCES public.enf_case(id) ON DELETE CASCADE;



ALTER TABLE ONLY public.enf_selected_defendants
    ADD CONSTRAINT enf_selected_defendants_party_id_fkey FOREIGN KEY (party_id) REFERENCES public.party(id);



ALTER TABLE ONLY public.enf_warrant
    ADD CONSTRAINT enf_warrant_enf_case_id_fkey FOREIGN KEY (enf_case_id) REFERENCES public.enf_case(id) ON DELETE CASCADE;



ALTER TABLE ONLY public.enf_warrant_of_restitution
    ADD CONSTRAINT enf_warrant_of_restitution_enf_case_id_fkey FOREIGN KEY (enf_case_id) REFERENCES public.enf_case(id) ON DELETE CASCADE;



ALTER TABLE ONLY public.enf_writ
    ADD CONSTRAINT enf_writ_enf_case_id_fkey FOREIGN KEY (enf_case_id) REFERENCES public.enf_case(id) ON DELETE CASCADE;



ALTER TABLE ONLY public.enf_writ_of_restitution
    ADD CONSTRAINT enf_writ_of_restitution_enf_case_id_fkey FOREIGN KEY (enf_case_id) REFERENCES public.enf_case(id) ON DELETE CASCADE;



ALTER TABLE ONLY public.case_notification
    ADD CONSTRAINT fk_case_notification_case FOREIGN KEY (case_id) REFERENCES public.pcs_case(id);



ALTER TABLE ONLY public.case_notification
    ADD CONSTRAINT fk_case_notification_claim FOREIGN KEY (claim_id) REFERENCES public.claim(id);



ALTER TABLE ONLY public.case_notification
    ADD CONSTRAINT fk_case_notification_party FOREIGN KEY (party_id) REFERENCES public.party(id);



ALTER TABLE ONLY public.fee_payment
    ADD CONSTRAINT fk_fee_payment_claim FOREIGN KEY (possession_claim_id) REFERENCES public.claim(id);



ALTER TABLE ONLY public.fee_payment
    ADD CONSTRAINT fk_fee_payment_help_with_fees FOREIGN KEY (hwf_id) REFERENCES public.help_with_fees(id);



ALTER TABLE ONLY public.general_application
    ADD CONSTRAINT fk_general_application_party_case FOREIGN KEY (party_id, case_id) REFERENCES public.party(id, case_id);



ALTER TABLE ONLY public.regular_income_item
    ADD CONSTRAINT fk_income_item_regular_income FOREIGN KEY (regular_income_id) REFERENCES public.regular_income(id) ON DELETE CASCADE;



ALTER TABLE ONLY public.party_access_code
    ADD CONSTRAINT fk_party_access_code_party FOREIGN KEY (party_id, case_id) REFERENCES public.party(id, case_id);



ALTER TABLE ONLY public.regular_income
    ADD CONSTRAINT fk_regular_income_hc FOREIGN KEY (hc_id) REFERENCES public.household_circumstances(id) ON DELETE CASCADE;



ALTER TABLE ONLY public.general_application
    ADD CONSTRAINT general_application_case_id_fkey FOREIGN KEY (case_id) REFERENCES public.pcs_case(id);



ALTER TABLE ONLY public.general_application
    ADD CONSTRAINT general_application_hwf_id_fkey FOREIGN KEY (hwf_id) REFERENCES public.help_with_fees(id);



ALTER TABLE ONLY public.general_application
    ADD CONSTRAINT general_application_party_id_fkey FOREIGN KEY (party_id) REFERENCES public.party(id);



ALTER TABLE ONLY public.general_application
    ADD CONSTRAINT general_application_sot_id_fkey FOREIGN KEY (sot_id) REFERENCES public.statement_of_truth(id);



ALTER TABLE ONLY public.general_application
    ADD CONSTRAINT general_application_submission_document_id_fkey FOREIGN KEY (submission_document_id) REFERENCES public.document(id);



ALTER TABLE ONLY public.household_circumstances
    ADD CONSTRAINT household_circumstances_defendant_response_id_fkey FOREIGN KEY (defendant_response_id) REFERENCES public.defendant_response(id);



ALTER TABLE ONLY public.legal_representative
    ADD CONSTRAINT legal_representative_address_id_fkey FOREIGN KEY (address_id) REFERENCES public.address(id);



ALTER TABLE ONLY public.notice_of_possession
    ADD CONSTRAINT notice_of_possession_claim_id_fkey FOREIGN KEY (claim_id) REFERENCES public.claim(id);



ALTER TABLE ONLY public.party_access_code
    ADD CONSTRAINT party_access_code_case_id_fkey FOREIGN KEY (case_id) REFERENCES public.pcs_case(id);



ALTER TABLE ONLY public.party
    ADD CONSTRAINT party_address_id_fkey FOREIGN KEY (address_id) REFERENCES public.address(id);



ALTER TABLE ONLY public.party_attribute_assertion
    ADD CONSTRAINT party_attribute_assertion_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.party(id);



ALTER TABLE ONLY public.party_attribute_assertion
    ADD CONSTRAINT party_attribute_assertion_evidence_document_id_fkey FOREIGN KEY (evidence_document_id) REFERENCES public.document(id);



ALTER TABLE ONLY public.party_attribute_assertion
    ADD CONSTRAINT party_attribute_assertion_last_updated_by_fkey FOREIGN KEY (last_updated_by) REFERENCES public.party(id);



ALTER TABLE ONLY public.party_attribute_assertion
    ADD CONSTRAINT party_attribute_assertion_party_id_fkey FOREIGN KEY (party_id) REFERENCES public.party(id);



ALTER TABLE ONLY public.party
    ADD CONSTRAINT party_case_id_fkey FOREIGN KEY (case_id) REFERENCES public.pcs_case(id);



ALTER TABLE ONLY public.party
    ADD CONSTRAINT party_contact_preferences_id_fkey FOREIGN KEY (contact_preferences_id) REFERENCES public.contact_preferences(id);



ALTER TABLE ONLY public.payment_agreement
    ADD CONSTRAINT payment_agreement_defendant_response_id_fkey FOREIGN KEY (defendant_response_id) REFERENCES public.defendant_response(id);



ALTER TABLE ONLY public.pcs_case
    ADD CONSTRAINT pcs_case_property_address_id_fkey FOREIGN KEY (property_address_id) REFERENCES public.address(id);



ALTER TABLE ONLY public.possession_alternatives
    ADD CONSTRAINT possession_alternatives_claim_id_fkey FOREIGN KEY (claim_id) REFERENCES public.claim(id);



ALTER TABLE ONLY public.reasonable_adjustments
    ADD CONSTRAINT reasonable_adjustments_defendant_response_id_fkey FOREIGN KEY (defendant_response_id) REFERENCES public.defendant_response(id);



ALTER TABLE ONLY public.regular_expenses
    ADD CONSTRAINT regular_expenses_hc_id_fkey FOREIGN KEY (hc_id) REFERENCES public.household_circumstances(id);



ALTER TABLE ONLY public.rent_arrears
    ADD CONSTRAINT rent_arrears_claim_id_fkey FOREIGN KEY (claim_id) REFERENCES public.claim(id);



ALTER TABLE ONLY public.statement_of_truth
    ADD CONSTRAINT statement_of_truth_claim_id_fkey FOREIGN KEY (claim_id) REFERENCES public.claim(id);



ALTER TABLE ONLY public.tenancy_licence
    ADD CONSTRAINT tenancy_licence_case_id_fkey FOREIGN KEY (case_id) REFERENCES public.pcs_case(id);




