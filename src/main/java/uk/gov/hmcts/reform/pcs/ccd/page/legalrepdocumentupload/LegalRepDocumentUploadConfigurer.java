package uk.gov.hmcts.reform.pcs.ccd.page.legalrepdocumentupload;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.common.PageConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication.SelectParty;

@Component
@AllArgsConstructor
public class LegalRepDocumentUploadConfigurer implements PageConfigurer {

    private final UploadAdditionalDocumentsPage uploadAdditionalDocumentsPage;

    @Override
    public void configurePages(PageBuilder pageBuilder) {
        pageBuilder
            .add(new SelectParty())
            .add(new UploadAdditionalDocumentsInformationPage())
            .add(new ExistingApplicationPage())
            .add(uploadAdditionalDocumentsPage);
    }
}
