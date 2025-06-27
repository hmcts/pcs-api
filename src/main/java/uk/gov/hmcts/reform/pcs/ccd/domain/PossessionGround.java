package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@Getter
public enum PossessionGround implements HasLabel {

    RENT_ARREARS("Rent arrears (ground 8)"),
    RENT_TO_BUY("Sale of dwelling-house under rent-to-buy (ground 1B)"),
    SALE_BY_MORTGAGEE("Sale by mortgagee (ground 2)"),
    SUPERIOR_LEASE_END("Possession when superior lease ends (ground 2ZA)"),
    SUPERIOR_LANDLORD_POSSESSION("Possession by superior landlord (ground 2ZD)"),
    STUDENT_ACCOMMODATION("Student accommodation (ground 4)");

    private final String label;

    PossessionGround(String label) {
        this.label = label;
    }

}
