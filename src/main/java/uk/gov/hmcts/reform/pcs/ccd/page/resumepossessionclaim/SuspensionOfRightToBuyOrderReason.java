package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuy;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Component
public class SuspensionOfRightToBuyOrderReason implements CcdPageConfiguration {

    private final TextAreaValidationService textAreaValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("suspensionOfRightToBuyOrderReason", this::midEvent)
            .pageLabel("Reasons for requesting a suspension order")
            .showCondition("alternativesToPossession=\"SUSPENSION_OF_RIGHT_TO_BUY\"")
            .label("suspensionOfRightToBuyOrderReason-info", "---")
                .complex(PCSCase::getSuspensionOfRightToBuy)
                .mandatory(SuspensionOfRightToBuy::getSuspensionOfRightToBuyReason)
                .done();
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        
        SuspensionOfRightToBuy suspensionOfRightToBuy = caseData.getSuspensionOfRightToBuy();
        if (suspensionOfRightToBuy != null) {
            List<String> validationErrors = textAreaValidationService.validateSingleTextArea(
                suspensionOfRightToBuy.getSuspensionOfRightToBuyReason(),
                "Why are you requesting a suspension order?",
                TextAreaValidationService.SHORT_TEXT_LIMIT
            );
            
            return textAreaValidationService.createValidationResponse(caseData, validationErrors);
        }
        
        return textAreaValidationService.createValidationResponse(caseData, new ArrayList<>());
    }
}
