package uk.gov.hmcts.reform.pcs.ccd.domain.grounds;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.CCD;

/**
 * Enum representing rent arrears grounds for possession claims.
 */
@AllArgsConstructor
@Getter
public enum AssuredRentArrearsGround {

    @CCD(label = "Serious rent arrears (ground 8)")
    SERIOUS_RENT_ARREARS_GROUND8("Serious rent arrears (ground 8)"),

    @CCD(label = "Rent arrears (ground 10)")
    RENT_ARREARS_GROUND10("Rent arrears (ground 10)"),

    @CCD(label = "Persistent delay in paying rent (ground 11)")
    PERSISTENT_DELAY_GROUND11("Persistent delay in paying rent (ground 11)");

    private final String label;
}
