package uk.gov.hmcts.reform.pcs.ccd.page.claimantmakeanapplication;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.XuiGenAppRequest;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.featureFlagsEnabled;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.XUI_WELSH_LANGUAGE_USED;

@Slf4j
@AllArgsConstructor
public class WhichLanguage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("whichLanguage")
            .pageLabel("Which language did you use to complete this service?")
            .showCondition(featureFlagsEnabled(XUI_WELSH_LANGUAGE_USED))
            .readonly(PCSCase::getFeatureFlags, NEVER_SHOW, true)
            .label("cma-whichLanguage-lineSeparator", "---")
            .complex(PCSCase::getXuiGenAppRequest)
            .mandatory(XuiGenAppRequest::getLanguageUsed)
            .done();
    }


}
