CREATE TYPE income_type AS ENUM (
    'INCOME_FROM_JOBS',
    'PENSION',
    'UNIVERSAL_CREDIT',
    'OTHER_BENEFITS',
    'MONEY_FROM_ELSEWHERE'
);

CREATE TYPE recurrence_frequency AS ENUM (
    'WEEKLY',
    'MONTHLY'
);

CREATE TABLE regular_income (
    id UUID PRIMARY KEY,
    hc_id UUID NOT NULL,
    other_income_details VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_regular_income_hc FOREIGN KEY (hc_id)
        REFERENCES household_circumstances(id) ON DELETE CASCADE,
    CONSTRAINT uq_regular_income_hc UNIQUE (hc_id)
);

CREATE INDEX idx_regular_income_hc_id ON regular_income(hc_id);

CREATE TABLE regular_income_item (
    id UUID PRIMARY KEY,
    regular_income_id UUID NOT NULL,
    income_type income_type NOT NULL,
    amount DECIMAL(18,2),
    frequency recurrence_frequency,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_income_item_regular_income FOREIGN KEY (regular_income_id)
        REFERENCES regular_income(id) ON DELETE CASCADE,
    CONSTRAINT chk_income_item_amount CHECK (amount IS NULL OR amount >= 0),
    CONSTRAINT uq_income_item_type UNIQUE (regular_income_id, income_type)
);

CREATE INDEX idx_income_item_regular_income_id ON regular_income_item(regular_income_id);

ALTER TABLE household_circumstances DROP COLUMN IF EXISTS regular_income;
