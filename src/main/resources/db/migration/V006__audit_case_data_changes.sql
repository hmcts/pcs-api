create trigger ccd_audit_row_changes
    after insert or update or delete on pcs_case
    for each row execute function ccd.audit_row_change();

create trigger ccd_audit_row_changes
    after insert or update or delete on case_review_date
    for each row execute function ccd.audit_row_change();

create trigger ccd_audit_row_changes
    after insert or update or delete on address
    for each row execute function ccd.audit_row_change();

create trigger ccd_audit_row_changes
    after insert or update or delete on claim_party
    for each row execute function ccd.audit_row_change();

create trigger ccd_audit_row_changes
    after insert or update or delete on claim_ground
    for each row execute function ccd.audit_row_change();

create trigger ccd_audit_row_changes
    after insert or update or delete on claim_document
    for each row execute function ccd.audit_row_change();

create trigger ccd_audit_row_changes
    after insert or update or delete on asb_prohibited_conduct
    for each row execute function ccd.audit_row_change();

create trigger ccd_audit_row_changes
    after insert or update or delete on notice_of_possession
    for each row execute function ccd.audit_row_change();

create trigger ccd_audit_row_changes
    after insert or update or delete on possession_alternatives
    for each row execute function ccd.audit_row_change();

create trigger ccd_audit_row_changes
    after insert or update or delete on rent_arrears
    for each row execute function ccd.audit_row_change();

create trigger ccd_audit_row_changes
    after insert or update or delete on statement_of_truth
    for each row execute function ccd.audit_row_change();

create trigger ccd_audit_row_changes
    after insert or update or delete on tenancy_licence
    for each row execute function ccd.audit_row_change();

create trigger ccd_audit_row_changes
    after insert or update or delete on help_with_fees
    for each row execute function ccd.audit_row_change();

create trigger ccd_audit_row_changes
    after insert or update or delete on case_note
    for each row execute function ccd.audit_row_change();

create trigger ccd_audit_row_changes
    after insert or update or delete on case_link
    for each row execute function ccd.audit_row_change();

create trigger ccd_audit_row_changes
    after insert or update or delete on case_link_reason
    for each row execute function ccd.audit_row_change();

create trigger ccd_audit_row_changes
    after insert or update or delete on general_application
    for each row execute function ccd.audit_row_change();

create trigger ccd_audit_row_changes
    after insert or update or delete on case_flag
    for each row execute function ccd.audit_row_change();

create trigger ccd_audit_row_changes
    after insert or update or delete on case_party_flag
    for each row execute function ccd.audit_row_change();

create trigger ccd_audit_row_changes
    after insert or update or delete on claim
    for each row execute function ccd.audit_row_change();

create trigger ccd_audit_row_changes
    after insert or update or delete on claim_activity_log
    for each row execute function ccd.audit_row_change();

create trigger ccd_audit_row_changes
    after insert or update or delete on claim_party_legal_representative
    for each row execute function ccd.audit_row_change();

create trigger ccd_audit_row_changes
    after insert or update or delete on contact_preferences
    for each row execute function ccd.audit_row_change();

create trigger ccd_audit_row_changes
    after insert or update or delete on counter_claim
    for each row execute function ccd.audit_row_change();

create trigger ccd_audit_row_changes
    after insert or update or delete on counter_claim_party
    for each row execute function ccd.audit_row_change();

create trigger ccd_audit_row_changes
    after insert or update or delete on defendant_response
    for each row execute function ccd.audit_row_change();

create trigger ccd_audit_row_changes
    after insert or update or delete on document
    for each row execute function ccd.audit_row_change();

create trigger ccd_audit_row_changes
    after insert or update or delete on enf_case
    for each row execute function ccd.audit_row_change();

create trigger ccd_audit_row_changes
    after insert or update or delete on enf_risk_profile
    for each row execute function ccd.audit_row_change();

create trigger ccd_audit_row_changes
    after insert or update or delete on enf_selected_defendants
    for each row execute function ccd.audit_row_change();

create trigger ccd_audit_row_changes
    after insert or update or delete on enf_warrant
    for each row execute function ccd.audit_row_change();

create trigger ccd_audit_row_changes
    after insert or update or delete on enf_warrant_of_restitution
    for each row execute function ccd.audit_row_change();

create trigger ccd_audit_row_changes
    after insert or update or delete on enf_writ
    for each row execute function ccd.audit_row_change();

create trigger ccd_audit_row_changes
    after insert or update or delete on enf_writ_of_restitution
    for each row execute function ccd.audit_row_change();

create trigger ccd_audit_row_changes
    after insert or update or delete on fee_payment
    for each row execute function ccd.audit_row_change();

create trigger ccd_audit_row_changes
    after insert or update or delete on household_circumstances
    for each row execute function ccd.audit_row_change();

create trigger ccd_audit_row_changes
    after insert or update or delete on legal_representative
    for each row execute function ccd.audit_row_change();

create trigger ccd_audit_row_changes
    after insert or update or delete on party
    for each row execute function ccd.audit_row_change();

create trigger ccd_audit_row_changes
    after insert or update or delete on party_access_code
    for each row execute function ccd.audit_row_change();

create trigger ccd_audit_row_changes
    after insert or update or delete on party_attribute_assertion
    for each row execute function ccd.audit_row_change();

create trigger ccd_audit_row_changes
    after insert or update or delete on payment_agreement
    for each row execute function ccd.audit_row_change();

create trigger ccd_audit_row_changes
    after insert or update or delete on reasonable_adjustments
    for each row execute function ccd.audit_row_change();

create trigger ccd_audit_row_changes
    after insert or update or delete on regular_expenses
    for each row execute function ccd.audit_row_change();

create trigger ccd_audit_row_changes
    after insert or update or delete on regular_income
    for each row execute function ccd.audit_row_change();

create trigger ccd_audit_row_changes
    after insert or update or delete on regular_income_item
    for each row execute function ccd.audit_row_change();
