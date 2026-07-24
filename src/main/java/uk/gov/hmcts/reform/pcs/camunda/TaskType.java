package uk.gov.hmcts.reform.pcs.camunda;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TaskType {
    NEW_CLAIM_CREATE_NEW_HEARING("NewClaimCreateNewHearing", "New Claim –  Create new hearing", 5);

    private final String id;
    private final String name;
    private final Integer workingDays;
}
