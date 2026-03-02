package uk.gov.hmcts.reform.pcs.ccd.domain.grounds;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.reform.pcs.ccd.domain.PossessionGroundEnum;

@AllArgsConstructor
@Getter
public enum IntroductoryDemotedOrOtherNoGrounds implements PossessionGroundEnum {

    NO_GROUNDS("No grounds");

    private final String label;

    @Override
    public int getRank() {
        return this.ordinal();
    }

}
