package uk.gov.hmcts.reform.pcs.ccd.page.documentamend;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentamend.DocumentAmendDetails;

import java.time.Clock;
import java.time.LocalDate;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

@Component
public class AmendDocumentDetailsPage implements CcdPageConfiguration {

    private static final String FUTURE_ISSUE_DATE_ERROR = "Issue date must be today or in the past";
    private static final String PAGE_ID = "amendDocumentDetails";

    private final Clock ukClock;

    public AmendDocumentDetailsPage(@Qualifier("ukClock") Clock ukClock) {
        this.ukClock = ukClock;
    }

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page(PAGE_ID, this::midEvent)
            .pageLabel("Amend document details")
            .label(PAGE_ID + "-separator", "---")
            .complex(PCSCase::getDocumentAmendDetails)
                .mandatory(DocumentAmendDetails::getAmendedFileName)
                .readonly(DocumentAmendDetails::getShowRelatedSubmissionsList, NEVER_SHOW, true)
                .mandatory(
                    DocumentAmendDetails::getRelatedSubmission,
                    "documentAmend_ShowRelatedSubmissionsList=\"YES\""
                )
                .mandatory(
                    DocumentAmendDetails::getRelatedSubmissionsDocumentType,
                    "documentAmend_RelatedSubmission=\"NONE\" "
                        + "AND documentAmend_ShowRelatedSubmissionsList=\"YES\""
                )
                .mandatory(
                    DocumentAmendDetails::getStandaloneDocumentType,
                    "documentAmend_ShowRelatedSubmissionsList!=\"YES\""
                )
                .optional(DocumentAmendDetails::getIssueDate)
                .mandatory(DocumentAmendDetails::getRelatedParty)
            .done();
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        LocalDate issueDate = caseData.getDocumentAmendDetails() == null
            ? null
            : caseData.getDocumentAmendDetails().getIssueDate();

        if (issueDate != null && issueDate.isAfter(LocalDate.now(ukClock))) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(caseData)
                .errorMessageOverride(FUTURE_ISSUE_DATE_ERROR)
                .build();
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }
}
