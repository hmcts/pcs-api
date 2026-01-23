package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

public class ChooseGeneralApplicationPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("chooseGenApplication")
            .pageLabel("Choose an application")
            .label("chooseGenApplication-content", "---")
            .complex(PCSCase::getEnforcementOrder)
            .mandatory(EnforcementOrder::getGeneralApplicationTypes)
            .done()
            .label("chooseGenApplication-save-and-return", SAVE_AND_RETURN);
    }
}
