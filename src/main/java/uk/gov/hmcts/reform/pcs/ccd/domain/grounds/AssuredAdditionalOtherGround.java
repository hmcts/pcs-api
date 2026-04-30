package uk.gov.hmcts.reform.pcs.ccd.domain.grounds;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.reform.pcs.ccd.domain.PossessionGroundEnum;

@AllArgsConstructor
@Getter
public enum AssuredAdditionalOtherGround implements PossessionGroundEnum {

    OTHER("Other");

    private final String label;

    @Override
    public int getRank() {
        return this.ordinal();
    }

}
