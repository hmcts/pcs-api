package uk.gov.hmcts.reform.pcs.ccd.page.nonprod;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

@AllArgsConstructor
@Component
@Slf4j
@Profile({"local", "dev", "preview"})
public class NonProdSupportPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("nonProdSupport")
            .pageLabel("A Non Production Support Page")
            .mandatory(PCSCase::getNonProdSupportFileList);
    }

}
