package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsWarrantOrWrit;

public class ConfirmHiringEnforcementOfficerPlaceholder implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("confirmHiringEnforcementOfficer")
            .pageLabel("Confirm if you have already hired a High Court enforcement officer (place holder)")
            .showCondition(ShowConditionsWarrantOrWrit.WRIT_FLOW)
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getWritDetails)
            .label("enforcementOfficerPlaceHolder-content", "---")
            .label("enforcementOfficerPlaceHolder-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }
}
