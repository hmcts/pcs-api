package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.DemotionOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuy;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuyDemotionOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.TextValidationService;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Component
public class SuspensionToBuyDemotionOfTenancyOrderReasons implements CcdPageConfiguration {

    private final TextValidationService textValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("suspensionToBuyDemotionOfTenancyOrderReasons", this::midEvent)
            .pageLabel("Reasons for requesting a suspension order and a demotion order")
            .showCondition("suspensionToBuyDemotionOfTenancyPages=\"Yes\"")
            .label("suspensionToBuyDemotionOfTenancyOrderReasons-info", "---")
            .complex(PCSCase::getSuspensionOfRightToBuyDemotionOfTenancy)
                .mandatory(SuspensionOfRightToBuyDemotionOfTenancy::getSuspensionOrderReason)
                .mandatory(SuspensionOfRightToBuyDemotionOfTenancy::getDemotionOrderReason)
            .done()
            .label("suspensionToBuyDemotionOfTenancyOrderReasons-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        List<String> validationErrors = new ArrayList<>();

        SuspensionOfRightToBuyDemotionOfTenancy suspensionOfRightToBuyDemotionOfTenancy =
            caseData.getSuspensionOfRightToBuyDemotionOfTenancy();
        if (suspensionOfRightToBuyDemotionOfTenancy != null) {
            validationErrors.addAll(textValidationService.validateSingleTextArea(
                suspensionOfRightToBuyDemotionOfTenancy.getSuspensionOrderReason(),
                SuspensionOfRightToBuy.SUSPENSION_OF_RIGHT_TO_BUY_REASON_LABEL,
                TextValidationService.SHORT_TEXT_LIMIT
            ));

            validationErrors.addAll(textValidationService.validateSingleTextArea(
                suspensionOfRightToBuyDemotionOfTenancy.getDemotionOrderReason(),
                DemotionOfTenancy.DEMOTION_OF_TENANCY_REASON_LABEL,
                TextValidationService.SHORT_TEXT_LIMIT
            ));
        }

        if (!validationErrors.isEmpty()) {
            return textValidationService.createValidationResponse(caseData, validationErrors);
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }
}
