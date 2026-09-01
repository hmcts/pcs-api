package uk.gov.hmcts.reform.pcs.ccd.page.claimantmakeanapplication;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.XuiGenAppRequest;

@Slf4j
@AllArgsConstructor
public class DocumentUploadWanted implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("documentUploadWanted")
            .pageLabel("Confirm if you want to upload documents to support your application")
            .label("documentUploadWanted-lineSeparator", "---")
            .complex(PCSCase::getXuiGenAppRequest)
            .mandatory(XuiGenAppRequest::getHasSupportingDocuments, null, null,
                       "Do you want to upload documents to support your application?")
            .done();
    }

}
