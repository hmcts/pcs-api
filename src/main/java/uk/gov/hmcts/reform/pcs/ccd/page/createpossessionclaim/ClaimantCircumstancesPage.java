package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

@AllArgsConstructor
@Component
public class ClaimantCircumstancesPage implements CcdPageConfiguration {

    private final TextAreaValidationService textAreaValidationService;

    private static final String YOU_CAN_ENTER_UP_TO_950_CHARACTERS = "You can enter up to 950 characters";
    private static final String SHOW_CONDITION = "claimantCircumstancesSelect=\"YES\"";
    private static final String CLAIMANT_CIRCUMSTANCES_INFO = "claimantCircumstances-Info";
    private static final String CLAIMANT_CIRCUMSTANCES = "claimantCircumstances";
    private static final String CLAIMANT_CIRCUMSTANCES_LABEL = "Claimant circumstances";
    private static final String GIVE_DETAILS_ABOUT_THE_CLAIMANT_NAME_CIRCUMSTANCES
        = "Give details about ${claimantNamePossessiveForm} circumstances";

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page(CLAIMANT_CIRCUMSTANCES, this::midEvent)
            .pageLabel(CLAIMANT_CIRCUMSTANCES_LABEL)
            .complex(PCSCase::getClaimantCircumstances)
                .readonly(ClaimantCircumstances::getClaimantNamePossessiveForm, NEVER_SHOW)
                .label(
                    CLAIMANT_CIRCUMSTANCES_INFO, """
                    ---
                    """)
                .mandatory(ClaimantCircumstances::getClaimantCircumstancesSelect)
                .mandatory(ClaimantCircumstances::getClaimantCircumstancesDetails, SHOW_CONDITION,
                           "",
                           GIVE_DETAILS_ABOUT_THE_CLAIMANT_NAME_CIRCUMSTANCES,
                           YOU_CAN_ENTER_UP_TO_950_CHARACTERS,
                           false
                ).done();
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        
        // Validate text area field for character limit - ultra simple approach
        ClaimantCircumstances claimantCircumstances = caseData.getClaimantCircumstances();
        if (claimantCircumstances != null) {
            // Use the actual dynamic label that's displayed to the user
            String dynamicLabel = "Give details about " + claimantCircumstances.getClaimantNamePossessiveForm() + "'s circumstances";
            
            List<String> validationErrors = textAreaValidationService.validateSingleTextArea(
                claimantCircumstances.getClaimantCircumstancesDetails(),
                dynamicLabel,
                950
            );
            
            return textAreaValidationService.createValidationResponse(caseData, validationErrors);
        }
        
        return textAreaValidationService.createValidationResponse(caseData, new ArrayList<>());
    }
}
