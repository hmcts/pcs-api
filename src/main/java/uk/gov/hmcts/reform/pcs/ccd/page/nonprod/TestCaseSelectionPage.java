package uk.gov.hmcts.reform.pcs.ccd.page.nonprod;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

@AllArgsConstructor
@Component
public class TestCaseSelectionPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("nonProdSupport")
            .pageLabel("A Non Production Support Page")
            .mandatory(PCSCase::getNonProdSupportFileList);
    }

}
