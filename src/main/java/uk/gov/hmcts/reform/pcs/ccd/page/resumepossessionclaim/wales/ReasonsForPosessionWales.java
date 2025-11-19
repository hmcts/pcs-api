package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractDiscretionaryGroundsWales;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

@Component
public class ReasonsForPosessionWales implements CcdPageConfiguration {
    // Placeholder for Wales reasons for possession page - full implementation will
    // be done in HDPI-2435

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("reasonsForPosessionWales", this::midEvent)
                .pageLabel("Reasons for possession (Wales - placeholder)")
                .label("reasonsForPosessionWales-separator", "---")
                .showCondition("legislativeCountry=\"Wales\" AND showReasonsForGroundsPageWales=\"Yes\"")
                .readonly(PCSCase::getShowReasonsForGroundsPageWales, NEVER_SHOW)
                .label("reasonsForPosessionWales-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
            CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        boolean hasASB = hasASBSelected(caseData);
        if (hasASB) {
            caseData.setShowASBQuestionsPageWales(YesOrNo.YES);
        } else {
            caseData.setShowASBQuestionsPageWales(YesOrNo.NO);
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(caseData)
                .build();
    }

    private boolean hasASBSelected(PCSCase caseData) {
        var discretionaryGrounds = caseData.getDiscretionaryGroundsWales();
        var secureDiscretionaryGrounds = caseData.getSecureContractDiscretionaryGroundsWales();

        boolean hasASBStandard = discretionaryGrounds != null
                && discretionaryGrounds.contains(DiscretionaryGroundWales.ANTISOCIAL_BEHAVIOUR_SECTION_157);
        boolean hasASBSecure = secureDiscretionaryGrounds != null
                && secureDiscretionaryGrounds.contains(SecureContractDiscretionaryGroundsWales.ANTISOCIAL_BEHAVIOUR);

        return hasASBStandard || hasASBSecure;
    }
}
