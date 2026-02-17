package uk.gov.hmcts.reform.pcs.ccd.domain.wales;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.reform.pcs.ccd.domain.PossessionGroundEnum;

@AllArgsConstructor
@Getter
public enum MandatoryGroundWales implements PossessionGroundEnum {

    FAILURE_TO_GIVE_UP_POSSESSION_S170(
        "Failure to give up possession on date specified in contract-holder’s notice (section 170)"),
    LANDLORD_NOTICE_PERIODIC_S178(
        "Landlord’s notice given in relation to periodic standard contract (section 178)"),
    SERIOUS_ARREARS_PERIODIC_S181(
        "Contract-holder under a periodic standard contract seriously in arrears with rent (section 181)"),
    LANDLORD_NOTICE_FT_END_S186(
        "Landlord’s notice in connection with end of fixed term given (section 186)"),
    SERIOUS_ARREARS_FIXED_TERM_S187(
        "Contract-holder under a fixed term standard contract seriously in arrears with rent (section 187)"),
    FAIL_TO_GIVE_UP_BREAK_NOTICE_S191(
        "Failure to give up possession on date specified in contract-holder’s break clause notice (section 191)"),
    LANDLORD_BREAK_CLAUSE_S199(
        "Notice given under a landlord’s break clause (section 199)"),
    CONVERTED_FIXED_TERM_SCH12_25B2(
        "Notice given in relation to end of converted fixed term standard contract (paragraph 25B(2) of Schedule 12)");

    private final String label;

    @Override
    public int getRank() {
        return this.ordinal();
    }

}
