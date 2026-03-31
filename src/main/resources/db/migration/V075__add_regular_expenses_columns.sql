ALTER TABLE household_circumstances
    ADD COLUMN household_bills YES_NO,
    ADD COLUMN household_bills_amount DECIMAL(18,2),
    ADD COLUMN household_bills_frequency VARCHAR(10),

    ADD COLUMN loan_payments YES_NO,
    ADD COLUMN loan_payments_amount DECIMAL(18,2),
    ADD COLUMN loan_payments_frequency VARCHAR(10),

    ADD COLUMN child_spousal_maintenance YES_NO,
    ADD COLUMN child_spousal_maintenance_amount DECIMAL(18,2),
    ADD COLUMN child_spousal_maintenance_frequency VARCHAR(10),

    ADD COLUMN mobile_phone YES_NO,
    ADD COLUMN mobile_phone_amount DECIMAL(18,2),
    ADD COLUMN mobile_phone_frequency VARCHAR(10),

    ADD COLUMN grocery_shopping YES_NO,
    ADD COLUMN grocery_shopping_amount DECIMAL(18,2),
    ADD COLUMN grocery_shopping_frequency VARCHAR(10),

    ADD COLUMN fuel_transport YES_NO,
    ADD COLUMN fuel_transport_amount DECIMAL(18,2),
    ADD COLUMN fuel_transport_frequency VARCHAR(10),

    ADD COLUMN school_costs YES_NO,
    ADD COLUMN school_costs_amount DECIMAL(18,2),
    ADD COLUMN school_costs_frequency VARCHAR(10),

    ADD COLUMN clothing YES_NO,
    ADD COLUMN clothing_amount DECIMAL(18,2),
    ADD COLUMN clothing_frequency VARCHAR(10),

    ADD COLUMN other_expenses YES_NO,
    ADD COLUMN other_expenses_amount DECIMAL(18,2),
    ADD COLUMN other_expenses_frequency VARCHAR(10);
