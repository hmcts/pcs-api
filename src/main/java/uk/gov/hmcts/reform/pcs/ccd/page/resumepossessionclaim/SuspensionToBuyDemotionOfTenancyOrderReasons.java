package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuyDemotionOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Component
public class SuspensionToBuyDemotionOfTenancyOrderReasons implements CcdPageConfiguration {

    private final TextAreaValidationService textAreaValidationService;

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
            .done();
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        
        // Validate all text area fields for character limit
        List<String> validationErrors = new ArrayList<>();
        
        SuspensionOfRightToBuyDemotionOfTenancy suspensionOfRightToBuyDemotionOfTenancy =
            caseData.getSuspensionOfRightToBuyDemotionOfTenancy();
        if (suspensionOfRightToBuyDemotionOfTenancy != null) {
            validationErrors.addAll(textAreaValidationService.validateMultipleTextAreas(
                TextAreaValidationService.FieldValidation.of(
                    suspensionOfRightToBuyDemotionOfTenancy.getSuspensionOrderReason(),
                    "Why are you requesting a suspension order?",
                    TextAreaValidationService.SHORT_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    suspensionOfRightToBuyDemotionOfTenancy.getDemotionOrderReason(),
                    "Why are you requesting a demotion order?",
                    TextAreaValidationService.SHORT_TEXT_LIMIT
                )
            ));
        }
        
        return textAreaValidationService.createValidationResponse(caseData, validationErrors);
    }
}
