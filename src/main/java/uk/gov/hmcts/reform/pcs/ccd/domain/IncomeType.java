package uk.gov.hmcts.reform.pcs.ccd.domain;

import uk.gov.hmcts.ccd.sdk.api.HasLabel;

public enum IncomeType implements HasLabel {
    INCOME_FROM_JOBS("Income from jobs"),
    PENSION("Pension"),
    UNIVERSAL_CREDIT("Universal credit"),
    OTHER_BENEFITS("Other benefits"),
    MONEY_FROM_ELSEWHERE("Money from elsewhere");

    private final String label;

    IncomeType(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
