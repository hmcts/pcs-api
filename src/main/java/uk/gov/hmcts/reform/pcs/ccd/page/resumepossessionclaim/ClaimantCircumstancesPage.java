package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.TextValidationService;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

@AllArgsConstructor
@Component
public class ClaimantCircumstancesPage implements CcdPageConfiguration {

    private final TextValidationService textValidationService;

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
                ).done()
            .label("claimantCircumstances-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        List<String> validationErrors = new ArrayList<>();

        ClaimantCircumstances claimantCircumstances = caseData.getClaimantCircumstances();
        if (claimantCircumstances != null) {
            String dynamicLabel = "Give details about "
                + claimantCircumstances.getClaimantNamePossessiveForm()
                + "’s circumstances";

            validationErrors.addAll(textValidationService.validateSingleTextArea(
                claimantCircumstances.getClaimantCircumstancesDetails(),
                dynamicLabel,
                TextValidationService.LONG_TEXT_LIMIT
            ));
        }

        return textValidationService.createValidationResponse(caseData, validationErrors);
    }
}
