package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GAType {

    STRIKE_OUT("Strike out"),
    SUMMARY_JUDGEMENT("Summary judgment"),
    STAY_THE_CLAIM("Stay the claim"),
    EXTEND_TIME("Extend time"),
    ADJOURN_HEARING("Adjourn a hearing"),
    UNLESS_ORDER("Unless order"),
    OTHER("Other");


    private final String displayedValue;
}
