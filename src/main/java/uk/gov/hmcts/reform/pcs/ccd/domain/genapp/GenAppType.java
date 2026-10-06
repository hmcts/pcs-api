package uk.gov.hmcts.reform.pcs.ccd.domain.genapp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@Getter
@AllArgsConstructor
public enum GenAppType implements HasLabel {

    ADJOURN("Adjourn (delay) the hearing - You can apply to change the defendant’s court hearing "
                + "until a later time or date", "Adjourn the hearing"),

    SET_ASIDE("Ask the court to set aside (cancel) a decision the court has made - You can ask "
                  + "the court to set aside its order if the defendant has a good reason. "
                  + "For example, if they were unable to attend the court hearing because they were ill",
              "Set aside an order"),

    SOMETHING_ELSE("Something else - Make an application for something that is not listed above", "Something else");

    private final String label;

    /** What the application is for, as court staff and judges name it: "Adjourn the hearing". */
    private final String shortName;

}
