CREATE TABLE regular_expenses(
 id UUID PRIMARY KEY,
 hc_id UUID NOT NULL REFERENCES household_circumstances(id),
 expense_type VARCHAR(30) NOT NULL,
 amount DECIMAL(18,2) NOT NULL,
 expense_frequency VARCHAR(10) NOT NULL,
 CONSTRAINT chk_regular_expense_amount_positive
   CHECK (amount >= 0)
);
