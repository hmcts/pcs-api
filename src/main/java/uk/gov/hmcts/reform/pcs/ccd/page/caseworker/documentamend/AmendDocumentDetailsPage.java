package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.documentamend;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentamend.DocumentAmendDetails;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;

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
                    "documentAmend_ShowRelatedSubmissionsList=\"YES\"",
                    true
                )
                .mandatory(
                    DocumentAmendDetails::getRelatedSubmissionsDocumentType,
                    "documentAmend_RelatedSubmission=\"NONE\" "
                        + "AND documentAmend_ShowRelatedSubmissionsList=\"YES\"",
                    true
                )
                .mandatory(
                    DocumentAmendDetails::getStandaloneDocumentType,
                    "documentAmend_ShowRelatedSubmissionsList!=\"YES\"",
                    true
                )
                .optional(DocumentAmendDetails::getIssueDate)
                .mandatory(DocumentAmendDetails::getRelatedParty)
                .readonly(DocumentAmendDetails::getRelatedPartyCode, NEVER_SHOW, true)
                .readonly(DocumentAmendDetails::getRelatedSubmissionCode, NEVER_SHOW, true)
                .readonly(DocumentAmendDetails::getRelatedSubmissionsDocumentTypeCode, NEVER_SHOW, true)
                .readonly(DocumentAmendDetails::getStandaloneDocumentTypeCode, NEVER_SHOW, true)
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

        persistSelectedCodes(caseData.getDocumentAmendDetails());

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }

    private void persistSelectedCodes(DocumentAmendDetails details) {
        if (details == null) {
            return;
        }

        details.setRelatedPartyCode(selectedPartyCode(details.getRelatedParty()));
        details.setRelatedSubmissionCode(selectedStringListCode(details.getRelatedSubmission()));
        details.setRelatedSubmissionsDocumentTypeCode(
            selectedStringListCode(details.getRelatedSubmissionsDocumentType())
        );
        details.setStandaloneDocumentTypeCode(selectedStringListCode(details.getStandaloneDocumentType()));
    }

    private String selectedPartyCode(DynamicList relatedParty) {
        return relatedParty == null || relatedParty.getValue() == null || relatedParty.getValue().getCode() == null
            ? null
            : relatedParty.getValue().getCode().toString();
    }

    private String selectedStringListCode(DynamicStringList dynamicStringList) {
        return dynamicStringList == null || dynamicStringList.getValue() == null
            ? null
            : dynamicStringList.getValue().getCode();
    }
}
