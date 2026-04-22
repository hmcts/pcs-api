package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

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
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
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
            .done()
            .label("additionalReasonsForPossession-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        
        List<String> validationErrors = new ArrayList<>();
        
        AdditionalReasons additionalReasons = caseData.getAdditionalReasonsForPossession();
        if (additionalReasons != null) {
            validationErrors.addAll(textAreaValidationService.validateSingleTextArea(
                additionalReasons.getReasons(),
                "Additional reasons for possession",
                TextAreaValidationService.EXTRA_LONG_TEXT_LIMIT
            ));
        }
        
        return textAreaValidationService.createValidationResponse(caseData, validationErrors);
    }

}
