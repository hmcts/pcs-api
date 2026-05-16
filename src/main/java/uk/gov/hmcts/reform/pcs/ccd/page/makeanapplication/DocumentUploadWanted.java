package uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.XuiGenAppRequest;

@Slf4j
@AllArgsConstructor
public class DocumentUploadWanted implements CcdPageConfiguration {

    private static final String INFO_MARKDOWN = """
        <p class="govuk-body">If you’ve selected multiple applications, you should
        upload documents that relate to all of them.</p>
        """;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("documentUploadWanted")
            .pageLabel("Do you want to upload documents to support the defendant’s application? (Optional)")
            .label("documentUploadWanted-lineSeparator", "---")
            .label("documentUploadWanted-info", INFO_MARKDOWN)
            .complex(PCSCase::getXuiGenAppRequest)
            .mandatory(XuiGenAppRequest::getHasSupportingDocuments)
            .done();
    }

}
