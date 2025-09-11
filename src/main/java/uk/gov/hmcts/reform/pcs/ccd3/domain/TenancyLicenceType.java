package uk.gov.hmcts.reform.pcs.ccd3.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum TenancyLicenceType implements HasLabel {

    ASSURED_TENANCY("Assured tenancy"),
    SECURE_TENANCY("Secure tenancy"),
    INTRODUCTORY_TENANCY("Introductory tenancy"),
    FLEXIBLE_TENANCY("Flexible tenancy"),
    DEMOTED_TENANCY("Demoted tenancy"),
    OTHER("Other");

    private final String label;

}
