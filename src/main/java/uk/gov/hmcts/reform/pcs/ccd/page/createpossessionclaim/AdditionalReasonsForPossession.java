package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalReasons;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo.YES;

@AllArgsConstructor
@Component
public class AdditionalReasonsForPossession implements CcdPageConfiguration {

    private final TextAreaValidationService textAreaValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("additionalReasonsForPossession", this::midEvent)
            .pageLabel("Additional reasons for possession")
            .label("additionalReasonsForPossession-separator", "---")
            .complex(PCSCase::getAdditionalReasonsForPossession)
                .mandatory(AdditionalReasons::getHasReasons)
                .mandatory(
                    AdditionalReasons::getReasons,
                    ShowConditions.fieldEquals("additionalReasonsForPossession.hasReasons", YES))
            .done();
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        
        // Validate text area field for character limit - ultra simple approach
        AdditionalReasons additionalReasons = caseData.getAdditionalReasonsForPossession();
        if (additionalReasons != null) {
            List<String> validationErrors = textAreaValidationService.validateSingleTextArea(
                additionalReasons.getReasons(),
                "Additional reasons for possession",
                6400
            );
            
            return textAreaValidationService.createValidationResponse(caseData, validationErrors);
        }
        
        return textAreaValidationService.createValidationResponse(caseData, new ArrayList<>());
    }

}
