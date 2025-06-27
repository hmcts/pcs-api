package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@Getter
public enum TenancyAgreementType implements HasLabel {

    ASSURED_SHORTHOLD("Assured shorthold tenancy (AST)"),
    FIXED_TERM("Fixed-term tenancy"),
    ASSURED("Assured tenancy"),
    STARTER("Starter tenancy"),
    INTRODUCTORY("Introductory tenancy"),
    SECURE("Secure tenancy"),
    FLEXIBLE("Flexible tenancy"),
    PERIODIC("Periodic tenancy");

    private final String label;

    TenancyAgreementType(String label) {
        this.label = label;
    }

}
