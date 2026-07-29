package uk.gov.hmcts.reform.pcs.camunda;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TaskType {
    NEW_CLAIM_CREATE_NEW_HEARING(
        "NewClaimCreateNewHearing",
        "New Claim – Create new hearing"
    ),
    REVIEW_ADDITIONAL_DOCS_GEN_APP(
        "ReviewAdditionalDocumentsGenApp",
        "Review additional documents – gen app"
    );

    private final String id;
    private final String name;
}
