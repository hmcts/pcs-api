-- reasonable_adjustments
CREATE TABLE reasonable_adjustments (
    id UUID PRIMARY KEY,
    defendant_response_id UUID NOT NULL REFERENCES defendant_response(id),
    reasonable_adjustments_required VARCHAR(250), --not null
    reasonable_adjustment_description VARCHAR(500),
    hearing_enhancement_description VARCHAR(250),
    sign_language_support_description VARCHAR(250),
    travel_support_description VARCHAR(250),
    welsh_language_requirements VARCHAR(250), --not null
    language_interpreter YES_NO, --not null
    language_support_description VARCHAR(250),
    considered_vulnerable YES_NO, --not null
    vulnerable_characteristic_description VARCHAR(250)
);


-- household_circumstances
CREATE TABLE household_circumstances (
     id UUID PRIMARY KEY,
     defendant_response_id UUID NOT NULL REFERENCES defendant_response(id),
     dependant_children YES_NO, --not null
     dependant_children_details VARCHAR(500),
     other_dependants YES_NO, --not null
     other_dependant_details VARCHAR(500), --not null
     other_tenants YES_NO,
     other_tenants_details VARCHAR(500), --not null
     alternative_accommodation YES_NO_NOT_SURE,
     alternative_accommodation_transfer_date DATE,
     share_additional_circumstances YES_NO, --not null
     additional_circumstances_details VARCHAR(500),
     exceptional_hardship YES_NO, --not null
     exceptional_hardship_details VARCHAR(500),
     share_income_expense_details YES_NO, --not null
     regular_income VARCHAR(60), --not null
     universal_credit YES_NO, --not null
     uc_application_date DATE,
     priority_debts YES_NO, --not null
     debt_total DECIMAL(18,2),
     debt_contribution VARCHAR(60),
     debt_contribution_frequency VARCHAR(60),
     regular_expenses VARCHAR(500), --not null
     expense_amount DECIMAL(18,2),
     expense_frequency VARCHAR(60)
);


-- payment_agreement
CREATE TABLE payment_agreement (
     id UUID PRIMARY KEY,
     defendant_response_id UUID NOT NULL REFERENCES defendant_response(id),
     any_payments_made YES_NO, --not null
     payment_details VARCHAR(500),
     paid_money_to_housing_org YES_NO,
     repayment_plan_agreed YES_NO,
     repayment_agreed_details VARCHAR(500),
     repay_arrears_instalments YES_NO, --not null
     additional_rent_contribution DECIMAL(18,2),
     additional_contribution_frequency VARCHAR(50)
);
