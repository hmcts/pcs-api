package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@Getter
@RequiredArgsConstructor
public enum GAType implements HasLabel {

    STRIKE_OUT("Strike out"),
    SUMMARY_JUDGEMENT("Summary judgment"),
    STAY_THE_CLAIM("Stay the claim"),
    EXTEND_TIME("Extend time"),
    ADJOURN_HEARING("Adjourn a hearing"),
    UNLESS_ORDER("Unless order"),
    OTHER("Other");

    private final String label;

    @Override
    public String getLabel() {
        return label;
    }
}
