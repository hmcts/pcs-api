package uk.gov.hmcts.reform.pcs.camunda;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TaskType {
    NEW_CLAIM_CREATE_NEW_HEARING(
        "NewClaimCreateNewHearing",
        "New Claim – Create new hearing",
        """
            List the case on List Assist, create and serve the Notice of Hearing on all parties, and upload a copy of
            the notice to the digital case file. Only mark the task as complete once all of these steps
            have been completed.
            """
    ),
    REVIEW_ADDITIONAL_DOCS_GEN_APP(
        "ReviewAdditionalDocumentsGenApp",
        "Review additional documents – gen app",
        """
            Review the additional documents submitted for the general application, check whether any further case
            action is required, and take the appropriate action. Only mark the task as complete once the documents
            have been reviewed and any required action has been completed.
            """
    ),
    REVIEW_ADJOURN_GEN_APP(
        "ReviewAdjournGenApp",
        "Review adjourn gen app",
        """
            Review the general application to adjourn the hearing, decide what action is needed, and take the
            appropriate action. Only mark the task as complete once the application has been reviewed and any
            required action has been completed.
            """
    ),
    REVIEW_SET_ASIDE_GEN_APP(
        "ReviewAdjournGenApp",
        "Review set aside gen app",
        """
            Review the general application to set aside, decide what action is needed, and take the appropriate
            action. Only mark the task as complete once the application has been reviewed and any required action
            has been completed.
            """
    ),
    REVIEW_GEN_APP(
        "ReviewGenApp",
            "Review gen app",
            """
            Review the general application, decide what action is needed, and take the appropriate action.
            Only mark the task as complete once the application has been reviewed and any required action
            has been completed.
            """
    ),
    REVIEW_DEFENDANT_RESPONSE_AND_COUNTERCLAIM(
        "ReviewDefendantResponseAndCounterclaim",
        "Review Defendant response and counterclaim",
        """
            Review the defendant’s response and counterclaim, check whether any further case action is required, and
            take the appropriate action before closing the task. Only mark the task as complete once the review is
            finished and any required action has been completed.
            """
    );

    private final String id;
    private final String name;
    private final String defaultDescription;
}
