package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.writ;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.ShowConditionsEnforcementType;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

public class BreathingSpaceWritPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("defendantInBreathingSpaceWrit")
            .pageLabel("Is the defendant in a breathing space moratorium?")
            .label("defendantInBreathingSpaceWrit-line-separator", "---")
            .showCondition(ShowConditionsEnforcementType.WRIT_FLOW)
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getWritDetails)
            .mandatory(WritDetails::getDefendantInBreathingSpace)
            .done()
            .done()
            .label("defendantInBreathingSpaceWrit-save-and-return", SAVE_AND_RETURN);
    }

}
