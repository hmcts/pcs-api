package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

public class BreathingSpacePage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("defendantInBreathingSpace")
            .pageLabel("Is the defendant in a breathing space moratorium?")
            .label("defendantInBreathingSpace-line-separator", "---")
            .showCondition(ShowConditionsEnforcementType.WARRANT_FLOW)
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getWarrantDetails)
            .mandatory(WarrantDetails::getDefendantInBreathingSpace)
            .done()
            .done()
            .label("defendantInBreathingSpace-save-and-return", SAVE_AND_RETURN);
    }

}
