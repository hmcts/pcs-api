package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum IntroductoryDemotedOrOtherNoGrounds implements HasLabel {

    NO_GROUNDS("No grounds");

    private final String label;
}
