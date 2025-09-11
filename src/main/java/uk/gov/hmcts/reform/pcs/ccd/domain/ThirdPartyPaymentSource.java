package uk.gov.hmcts.reform.pcs.ccd.domain;

import uk.gov.hmcts.ccd.sdk.api.HasLabel;

public enum ThirdPartyPaymentSource implements HasLabel {
    UNIVERSAL_CREDIT("Universal Credit"),
    HOUSING_BENEFIT("Housing Benefit"),
    DISCRETIONARY_HOUSING_PAYMENT("Discretionary Housing Payment"),
    HOMELESS_PREVENTION_FUND("Homeless Prevention Fund"),
    OTHER("Other");

    private final String label;

    ThirdPartyPaymentSource(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
