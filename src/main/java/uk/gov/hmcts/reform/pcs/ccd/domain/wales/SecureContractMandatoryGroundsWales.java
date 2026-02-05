package uk.gov.hmcts.reform.pcs.ccd.domain.wales;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.reform.pcs.ccd.domain.PossessionGroundEnum;

@AllArgsConstructor
@Getter
public enum SecureContractMandatoryGroundsWales implements PossessionGroundEnum {

    FAILURE_TO_GIVE_UP_POSSESSION_S170("Failure to give up possession on date "
            + "specified in contract-holder’s notice (section 170)"),
    LANDLORD_NOTICE_S186("Landlord’s notice in connection with end "
            + "of fixed term given (section 186)"),
    FAILURE_TO_GIVE_UP_POSSESSION_S191("Failure to give up possession on date specified "
            + "in contract-holder’s break clause notice (section 191)"),
    LANDLORD_NOTICE_S199("Notice given under a landlord’s break clause (section 199)"),;

    private final String label;

    @Override
    public int getRank() {
        return this.ordinal();
    }

}
