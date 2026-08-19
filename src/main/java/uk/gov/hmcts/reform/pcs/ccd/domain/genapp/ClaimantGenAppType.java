package uk.gov.hmcts.reform.pcs.ccd.domain.genapp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@Getter
@AllArgsConstructor
public enum ClaimantGenAppType implements HasLabel {

    ADJOURN("Adjourn (delay) the hearing - You can apply to change the claimant’s court hearing "
                + "until a later time or date"),

    SET_ASIDE("Ask the court to set aside (cancel) a decision the court has made - You can ask "
                  + "the court to set aside its order if you have a good reason. "
                  + "For example, if you were unable to attend the court hearing because your were ill"),

    SOMETHING_ELSE("Something else - Make an application for something that is not listed above");

    private final String label;

    public GenAppType toGenAppType() {
        return GenAppType.valueOf(this.name());
    }

}
