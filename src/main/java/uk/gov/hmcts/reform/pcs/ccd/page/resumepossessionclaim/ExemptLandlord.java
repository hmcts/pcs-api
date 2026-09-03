package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.WALES;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.and;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.featureFlagsDisabled;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.RELEASE_1_DOT_4;

@Component
public class ExemptLandlord implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("exemptLandlord")
            .pageLabel("Exempt landlord")
            .showCondition(and(WALES, featureFlagsDisabled(RELEASE_1_DOT_4)))
            .readonly(PCSCase::getFeatureFlags, NEVER_SHOW, true)
            .label("exemptLandlord-info", "---")
            .mandatory(PCSCase::getIsExemptLandlord)
            .label("exemptLandlord-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }
}
