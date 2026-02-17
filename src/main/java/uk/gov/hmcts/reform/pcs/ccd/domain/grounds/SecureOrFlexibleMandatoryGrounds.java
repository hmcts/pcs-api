package uk.gov.hmcts.reform.pcs.ccd.domain.grounds;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.reform.pcs.ccd.domain.PossessionGroundEnum;

@AllArgsConstructor
@Getter
public enum SecureOrFlexibleMandatoryGrounds implements PossessionGroundEnum {

    ANTI_SOCIAL("Antisocial behaviour");

    private final String label;

    @Override
    public int getRank() {
        return this.ordinal();
    }

}
