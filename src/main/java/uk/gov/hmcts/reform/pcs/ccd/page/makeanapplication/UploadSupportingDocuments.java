package uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.XuiGenAppRequest;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.fieldEquals;

@Slf4j
@AllArgsConstructor
public class UploadSupportingDocuments implements CcdPageConfiguration {

    private static final String INFO_MARKDOWN = """
        <p class="govuk-body">
         You should only upload documents that are relevant to the defendant’s application,
         such as a draft order or witness statement.
        </p>
        <p class="govuk-body">
         Before you upload the document, give it a name that tells the court what it is,
         for example ‘Job offer letter’.
        </p>
        <p class="govuk-body">
         Each document must be less than 100MB. You can upload the following file types: DOC/DOCX (Word),
         XLS/XLSM (Excel), PPT/PPTX (PowerPoint), PDF, RTF, TXT, CSV, JPG/JPEG, PNG, BMP, TIF/TIFF.
        </p>
        """;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("uploadSupportingDocuments")
            .pageLabel("Confirm if you want to upload documents to support the defendant’s application")
            .showCondition(fieldEquals("xui_genapp_HasSupportingDocuments", VerticalYesNo.YES))
            .label("uploadSupportingDocuments-lineSeparator", "---")
            .label("uploadSupportingDocuments-info", INFO_MARKDOWN)
            .complex(PCSCase::getXuiGenAppRequest)
            .mandatory(XuiGenAppRequest::getUploadedDocuments)
            .done();
    }

}
