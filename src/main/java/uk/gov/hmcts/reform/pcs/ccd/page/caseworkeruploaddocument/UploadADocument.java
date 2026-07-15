package uk.gov.hmcts.reform.pcs.ccd.page.caseworkeruploaddocument;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentupload.CaseworkerDocument;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

@Slf4j
@AllArgsConstructor
public class UploadADocument implements CcdPageConfiguration {

    private static final String INFO_MARKDOWN = """
        <h3 class="govuk-heading-m govuk-!-font-size-19">Before you upload the document</h3>
        <p class="govuk-body govuk-!-font-size-19">
        You must rename the file on your computer before uploading it here if it contains profanity, or there are
        other exceptional circumstances for why it should be renamed.
        </p>
        """;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("uploadADocument")
            .pageLabel("Upload a document")
            .label("uploadADocument-lineSeparator", "---")
            .label("uploadADocument-info", INFO_MARKDOWN)
            .complex(PCSCase::getCaseworkerDocument)
            .readonly(CaseworkerDocument::getShowRelatedSubmissionsList, NEVER_SHOW)
            .mandatory(CaseworkerDocument::getDocument)
            .mandatory(CaseworkerDocument::getRelatedSubmission,
                       ShowConditions.fieldEquals("cwDoc_ShowRelatedSubmissionsList", VerticalYesNo.YES))
            .mandatory(CaseworkerDocument::getRelatedSubmissionsDocumentType,
                       "cwDoc_RelatedSubmission=\"NONE\" AND cwDoc_ShowRelatedSubmissionsList=\"YES\"")
            .mandatory(CaseworkerDocument::getStandaloneDocumentType,
                       "cwDoc_ShowRelatedSubmissionsList!=\"YES\"")
            .optional(CaseworkerDocument::getIssueDate)
            .mandatory(CaseworkerDocument::getRelatedParty)
            .done();
    }

}
