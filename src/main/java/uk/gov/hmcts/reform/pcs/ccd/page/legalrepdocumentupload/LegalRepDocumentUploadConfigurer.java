package uk.gov.hmcts.reform.pcs.ccd.page.legalrepdocumentupload;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.common.PageConfigurer;

@Component
@AllArgsConstructor
public class LegalRepDocumentUploadConfigurer implements PageConfigurer {

    private final UploadAdditionalDocumentsPage uploadAdditionalDocumentsPage;

    @Override
    public void configurePages(PageBuilder pageBuilder) {
        pageBuilder
            .add(new UploadAdditionalDocumentsInformationPage())
            .add(new ExistingApplicationPage())
            .add(uploadAdditionalDocumentsPage)
            .add(new UploadedDocumentsPage());
    }
}
