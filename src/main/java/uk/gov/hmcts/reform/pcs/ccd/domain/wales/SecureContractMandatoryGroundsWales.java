package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum SecureContractMandatoryGroundsWales implements HasLabel {

    FAILURE_TO_GIVE_UP_POSSESSION_SECTION_170("Failure to give up possession on date "
            + "specified in contract-holder’s notice (section 170)"),
    LANDLORD_NOTICE_SECTION_186("Landlord’s notice in connection with end "
            + "of fixed term given (section 186)"),
    FAILURE_TO_GIVE_UP_POSSESSION_SECTION_191("Failure to give up possession on date specified "
            + "in contract-holder’s break clause notice (section 191)"),
    LANDLORD_NOTICE_SECTION_199("Notice given under a landlord’s break clause (section 199)"),;

    private final String label;

}
