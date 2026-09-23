package uk.gov.hmcts.reform.pcs.ccd.domain.hearing;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@Getter
@AllArgsConstructor
public enum HearingNoticeWording implements HasLabel {

    TPL("TPL - will take place on"),
    ADJ("ADJ - has been adjourned until"),
    RES("RES - has been restored to");

    private final String label;
}
