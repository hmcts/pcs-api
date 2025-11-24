package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

@Slf4j
public class ClaimantInformationPage implements CcdPageConfiguration {

    private static final String UPDATED_CLAIMANT_NAME_HINT = """
        Changing your claimant name here only updates it for this claim.
        It does not change your registered claimant name on My HMCTS
        """;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("claimantInformation", this::midEvent)
            .pageLabel("Claimant name")
            .label("claimantInformation-separator", "---")
            .readonlyWithLabel(PCSCase::getOrganisationName, "Your claimant name registered with My HMCTS is:")
            .mandatoryWithLabel(PCSCase::getIsClaimantNameCorrect,"Is this the correct claimant name?")
            .mandatory(PCSCase::getOverriddenClaimantName,
                    "isClaimantNameCorrect=\"NO\"",
                    null,
                    "What is the correct claimant name?",
                    UPDATED_CLAIMANT_NAME_HINT,
                    false)
            .label("claimantInformation-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);

    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        setClaimantNamePossessiveForm(details);

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }

    private void setClaimantNamePossessiveForm(CaseDetails<PCSCase, State> details) {
        PCSCase caseData = details.getData();
        log.debug("setClaimantNamePossessiveForm - overriddenClaimantName: {}", caseData.getOverriddenClaimantName());
        log.debug("setClaimantNamePossessiveForm - claimantName: {}", caseData.getClaimantName());
        
        // Initialize ClaimantCircumstances if it's null
        if (caseData.getClaimantCircumstances() == null) {
            log.debug("setClaimantNamePossessiveForm - ClaimantCircumstances is null, initializing");
            caseData.setClaimantCircumstances(ClaimantCircumstances.builder().build());
        }
        
        // Always recalculate claimantNamePossessiveForm based on current values
        // This ensures it's up-to-date even if it was lost or not persisted
        String claimantNamePossessiveForm = StringUtils.isNotEmpty(caseData.getOverriddenClaimantName())
            ? caseData.getOverriddenClaimantName()
            : caseData.getClaimantName();
        log.debug("setClaimantNamePossessiveForm - claimantNamePossessiveForm (before apostrophe): {}", claimantNamePossessiveForm);
        String finalPossessiveForm = applyApostrophe(claimantNamePossessiveForm);
        
        // Set the value (even if it already exists, to ensure it's current)
        caseData.getClaimantCircumstances().setClaimantNamePossessiveForm(finalPossessiveForm);
        log.debug("setClaimantNamePossessiveForm - final claimantNamePossessiveForm (after apostrophe): {}", finalPossessiveForm);
    }

    private String applyApostrophe(String value) {
        if (value == null) {
            return null;
        }
        return value.endsWith("s") || value.endsWith("S") ? value + "'" : value + "'s";
    }

}
