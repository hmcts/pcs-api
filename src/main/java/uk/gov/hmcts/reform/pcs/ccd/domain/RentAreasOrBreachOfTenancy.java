package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum RentAreasOrBreachOfTenancy implements HasLabel {

    RENT_ARREARS("Rent arears"),
    BREACH_OF_TENANCY("Breach of tenancy");

    private String label;
}
