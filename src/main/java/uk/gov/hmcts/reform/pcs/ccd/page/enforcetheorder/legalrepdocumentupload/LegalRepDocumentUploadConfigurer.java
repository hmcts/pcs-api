package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.legalrepdocumentupload;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.common.PageConfigurer;

@Component
@AllArgsConstructor
public class LegalRepDocumentUploadConfigurer implements PageConfigurer {

    @Override
    public void configurePages(PageBuilder pageBuilder) {
        pageBuilder
            .add(new UploadAdditionalDocumentsPage());
    }
}
