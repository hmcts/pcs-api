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
    REVIEW_CASE_FLAG(
        "ReviewCaseFlag",
        "Review Case Flag",
        """
            Review the case flags on the case flags tab, check whether any action is required, and take the appropriate
            action. Only mark the task as complete once the case flags have been reviewed and any required action has
            been completed.
            """
    ),
    REVIEW_CASE_FLAG_REQUEST(
        "ReviewCaseFlagRequest",
        "Review Case Flag request",
        """
            Review the case flag request, decide whether it should be approved or rejected, and record the decision.
            The task will close when the approval or rejection event is completed.
            """
    ),
    REVIEW_DATE_DUE(
        "ReviewDateDue",
        "Review Date Due",
        """
            Review the case note about the date due, check whether any follow-up action is required, and take the
            appropriate action. Only mark the task as complete once the date due has been reviewed and any required
            action has been completed.
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
    ),
    REVIEW_DEFENDANT_RESPONSE_AND_COUNTERCLAIM(
        "ReviewDefendantResponseAndCounterclaim",
        "Review Defendant response and counterclaim",
        """
            Review the defendant’s response and counterclaim, check whether any further case action is required, and
            take the appropriate action before closing the task. Only mark the task as complete once the review is
            finished and any required action has been completed.
            """
    ),
    TRANSLATE_DEFENDANT_SUBMITTED_DOCUMENT(
        "TranslateDefendantSubmittedDocument",
        "Translate Defendant Submitted Document",
        """
            Review the defendant submitted document in Case File View and arrange translation where required.
            Upload or record the translated document on the case file and take any further action needed.
            Only mark the task as complete once the translation has been completed and any required action has been
            taken.
            """
    ),
    REVIEW_CLAIMANT_WARRANT_APPLICATION(
        "ReviewClaimantWarrantApplication",
        "Review claimant warrant application",
        """
            Review the claimant's application for a warrant or writ and record the decision through the
            "Decide enforcement" event. Accepting it seals and issues the warrant or writ; rejecting it asks for
            a reason. The task closes when that event is submitted.
            """
    ),
    ALLOCATE_CASE_TO_BAILIFF(
        "AllocateCaseToBailiff",
        "Allocate case to bailiff",
        """
            Allocate the warrant to a bailiff at the court handling the case through the "Allocate case to
            bailiff" event. The task closes when that event is submitted.
            """
    ),
    REVIEW_RISKS_AND_SET_LEVEL(
        "ReviewRisksAndSetLevel",
        "Review risks and set level",
        """
            Read the claimant's risk assessment on the "Risk assessment" tab and record the eviction risk level
            through the "Set risk level" event. A high level needs a reason. The task closes when that event is
            submitted.
            """
    ),
    MARK_NOTICE_OF_EVICTION_SERVED(
        "MarkNoticeOfEvictionServed",
        "Mark notice of eviction as served",
        """
            Record when and how you served the notice of eviction through the "Mark notice of eviction as served"
            event. Serving the notice is what makes the enforcement visible to the defendants. The task closes
            when that event is submitted.
            """
    ),
    RESCHEDULE_EVICTION(
        "RescheduleEviction",
        "Reschedule eviction",
        """
            The claimant cannot attend the eviction on the scheduled date. Record a new date and time through the
            "Reschedule eviction" event, avoiding the dates the claimant has given. The task closes when that
            event is submitted.
            """
    ),
    REVIEW_UPDATED_RISK_INFORMATION(
        "ReviewUpdatedRiskInformation",
        "Review updated risk information",
        """
            The claimant has changed their risk assessment answers. Read the changes on the "Risk assessment" tab
            and confirm or change the risk level through the "Set risk level" event. The task closes when that
            event is submitted.
            """
    ),
    RECORD_WARRANT_RETURN(
        "RecordWarrantReturn",
        "Record warrant return",
        """
            Record the outcome of the eviction through the "Record warrant return" event. The return name decides
            whether the warrant closes, goes into breathing space, goes back for scheduling or reopens for
            allocation. The task closes when that event is submitted.
            """
    );

    private final String id;
    private final String name;
    private final String defaultDescription;
}
