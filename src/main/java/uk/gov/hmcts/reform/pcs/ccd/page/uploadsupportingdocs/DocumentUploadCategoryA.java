package uk.gov.hmcts.reform.pcs.ccd.page.uploadsupportingdocs;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Locale.ROOT;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;


public class DocumentUploadCategoryA implements CcdPageConfiguration {

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("pdf", "doc", "jpg", "jpeg");

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("documentUploadCategoryA", this::midEvent)
                .pageLabel("Upload supporting documents")
                .label("uploadLimits", """
                    Documents should be:
                    * uploaded separately, not as one large file
                    * a maximum of 100MB in size (larger files must be split)
                    * clearly labelled, e.g. applicant-name-evidence.pdf
                    * in PDF, JPG, JPEG or DOC format
                    """)
                .optional(PCSCase::getSupportingDocumentsCategoryA);
    }
    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        final PCSCase data = details.getData();
        List<ListValue<Document>> uploadedDocuments = data.getSupportingDocumentsCategoryA();
        List<String> errors = validateDocumentFormat(uploadedDocuments);

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(data)
            .errors(errors)
            .build();
    }

    private List<String> validateDocumentFormat(List<ListValue<Document>> uploadedDocuments) {
        List<String> errors = new ArrayList<>();

        if (uploadedDocuments == null || uploadedDocuments.isEmpty()) {
            return errors;
        }

        for (int i = 0; i < uploadedDocuments.size(); i++) {
            ListValue<Document> documentWrapper = uploadedDocuments.get(i);
            if (documentWrapper != null && documentWrapper.getValue() != null) {
                Document document = documentWrapper.getValue();
                String fileName = document.getFilename();

                if (fileName == null || fileName.trim().isEmpty()) {
                    errors.add("Document " + (i + 1) + " has no filename");
                    continue;
                }

                String fileExtension = substringAfterLast(fileName, ".");
                if (fileExtension.isEmpty()) {
                    errors.add("Document '" + fileName + "' has no file extension");
                } else if (!ALLOWED_EXTENSIONS.contains(fileExtension.toLowerCase(ROOT))) {
                    errors.add("Document '" + fileName + "' has an invalid file type. Only PDF, DOC, JPG, and JPEG files are allowed.");
                }
            }
        }

        return errors;
    }
}
