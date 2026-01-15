package uk.gov.hmcts.reform.pcs.ccd.page.testcasesupport;

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
            .page("testCaseSelection")
            .pageLabel("A Test Case Support Page")
            .mandatory(PCSCase::getTestCaseSupportFileList);
    }

}
