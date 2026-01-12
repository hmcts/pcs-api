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
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.TextValidationService;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Component
public class SuspensionOfRightToBuyOrderReason implements CcdPageConfiguration {

    private final TextValidationService textValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("suspensionOfRightToBuyOrderReason", this::midEvent)
            .pageLabel("Reasons for requesting a suspension order")
            .showCondition("alternativesToPossession=\"SUSPENSION_OF_RIGHT_TO_BUY\"")
            .label("suspensionOfRightToBuyOrderReason-info", "---")
                .complex(PCSCase::getSuspensionOfRightToBuy)
                .mandatory(SuspensionOfRightToBuy::getSuspensionOfRightToBuyReason)
                .done()
            .label("suspensionOfRightToBuyOrderReason-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        List<String> validationErrors = new ArrayList<>();

        SuspensionOfRightToBuy suspensionOfRightToBuy = caseData.getSuspensionOfRightToBuy();
        if (suspensionOfRightToBuy != null) {
            validationErrors.addAll(textValidationService.validateSingleTextArea(
                suspensionOfRightToBuy.getSuspensionOfRightToBuyReason(),
                SuspensionOfRightToBuy.SUSPENSION_OF_RIGHT_TO_BUY_REASON_LABEL,
                TextValidationService.SHORT_TEXT_LIMIT
            ));
        }

        return textValidationService.createValidationResponse(caseData, validationErrors);
    }
}
