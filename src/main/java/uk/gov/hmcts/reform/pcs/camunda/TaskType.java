package uk.gov.hmcts.reform.pcs.camunda;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TaskType {
    NEW_CLAIM_CREATE_NEW_HEARING(
        "NewClaimCreateNewHearing",
        "New Claim –  Create new hearing",
        5,
        "NewClaimCreateNewHearing"
    ),
    REVIEW_DEFENDANT_RESPONSE_AND_COUNTER_CLAIM(
        "ReviewDefendantResponseAndCounterclaim",
        "Review Defendant response and counterclaim",
        5,
        "ReviewDefendantResponseAndCounterclaim"
    );

    private final String id;
    private final String name;
    private final Integer workingDays;
    private final String processCategory;
}
