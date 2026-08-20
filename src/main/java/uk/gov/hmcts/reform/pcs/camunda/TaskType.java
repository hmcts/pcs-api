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
    TRANSLATE_CLAIMANT_SUBMITTED_DOCUMENT(
        "TranslateClaimantSubmittedDocument",
        "Translate Claimant Submitted Document",
        """
            Review the claimant submitted document in Case File View and arrange translation. Email the translated
            document to the court. Only mark the task as complete once the translation has been completed.
            """
    ),
    REVIEW_ADDITIONAL_DOCS_CLAIM(
        "ReviewAdditionalDocumentsClaim",
        "Review additional documents - claim",
        """
            Review the additional documents submitted on the claim, check whether any further case action is required,
            and take the appropriate action. Only mark the task as complete once the documents have been reviewed
            and any required action has been completed.
            """
    ),
    REVIEW_ADDITIONAL_DOCS_COUNTERCLAIM(
        "ReviewAdditionalDocumentsCounterclaim",
        "Review additional documents - counterclaim",
        """
            Review the additional documents submitted on the counterclaim, check whether any further case action is
            required, and take the appropriate action. Only mark the task as complete once the documents have been
            reviewed and any required action has been completed.
            """
    );

    private final String id;
    private final String name;
    private final String defaultDescription;
}
