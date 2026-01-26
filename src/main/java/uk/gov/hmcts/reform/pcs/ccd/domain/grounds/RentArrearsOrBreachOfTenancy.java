package uk.gov.hmcts.reform.pcs.ccd.domain.grounds;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum RentArrearsOrBreachOfTenancy implements HasLabel {

    RENT_ARREARS("Rent arrears"),
    BREACH_OF_TENANCY("Breach of the tenancy");

    private final String label;
}
