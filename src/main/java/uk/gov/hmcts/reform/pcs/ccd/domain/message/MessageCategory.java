package uk.gov.hmcts.reform.pcs.ccd.domain.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

/** What a message is about. A message about an application names which. */
@Getter
@AllArgsConstructor
public enum MessageCategory implements HasLabel {
    APPLICATION("Application"),
    HEARING("Hearing"),
    DOCUMENTS("Documents"),
    COUNTERCLAIM("Counterclaim"),
    ENFORCEMENT_ACTION("Enforcement action"),
    OTHER("Other");

    private final String label;
}
