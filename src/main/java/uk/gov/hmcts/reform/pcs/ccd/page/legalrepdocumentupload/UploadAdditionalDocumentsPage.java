package uk.gov.hmcts.reform.pcs.ccd.page.legalrepdocumentupload;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.LegalRepDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.LegalRepDocumentUploadDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.CcdPage;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;
import uk.gov.hmcts.reform.pcs.ccd.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
@AllArgsConstructor
public class UploadAdditionalDocumentsPage implements CcdPageConfiguration, CcdPage {

    private static final String DESCRIPTION_LABEL = "Short description";
    private final TextAreaValidationService textAreaValidationService;
    public static final String DOCUMENT_DETAILS_CONTENT  = """
                    <p class="govuk-body">
                        You should only upload documents that are relevant to your application.
                    </p>

                    <p class="govuk-body">
                        Before you upload the document, give it a name that tells the court what it is,
                        for example, ‘witness statement’, or ‘tenancy agreement’.
                    </p>

                    <p class="govuk-body">
                       You can upload the following file types: DOC/DOCX (Word), XLS/XLSM
                       (Excel), PPT/PPTX (PowerPoint), PDF, RTF, TXT, CSV, JPG/JPEG, PNG, BMP,
                       TIF/TIFF.
                    </p>

                    <p class="govuk-body govuk-!-font-weight-bold">
                        Before you upload your documents
                    </p>

                    <p class="govuk-body">
                        Give your document a name that explains what it is.
                    </p>

                    """;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        String pageKey = getPageKey();
        pageBuilder
            .page(pageKey)
            .pageLabel("Upload your documents")
            .label(pageKey + "-line-separator", "---")
            .label(pageKey + "-content", DOCUMENT_DETAILS_CONTENT)
            .complex(PCSCase::getLegalRepDocumentUploadDetails)
            .mandatory(LegalRepDocumentUploadDetails::getLegalRepDocuments)
            .done()
            .build();
    }

    @Override
    public String getPageKey() {
        return CcdPage.derivePageKey(this.getClass());
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(
        CaseDetails<PCSCase, State> caseDetails,
        CaseDetails<PCSCase, State> caseDetailsBefore) {

        PCSCase caseData = caseDetails.getData();
        LegalRepDocumentUploadDetails caseLegalRepDocs = caseData.getLegalRepDocumentUploadDetails();

        List<String> errors = validateDocumentDescription(caseLegalRepDocs.getLegalRepDocuments(), DESCRIPTION_LABEL);

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .errorMessageOverride(StringUtils.joinIfNotEmpty("\n", errors))
            .data(caseData)
            .build();
    }

    public List<String> validateDocumentDescription(
        List<ListValue<LegalRepDocument>> legalRepDocuments,
        String label) {

        List<String> validationErrorsList = new ArrayList<>();

        for (int i = 0; i < legalRepDocuments.size(); i++) {
            String documentDescription = legalRepDocuments.get(i).getValue().getDescription();
            String fieldLabel = "Legal Representative document %d".formatted(i + 1) + "'s " + label;
            validationErrorsList.addAll(textAreaValidationService.validateSingleTextArea(
                documentDescription, fieldLabel, TextAreaValidationService.EXTRA_SHORT_TEXT_LIMIT)
            );
        }
        return validationErrorsList;
    }
}
