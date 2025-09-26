package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

@Slf4j
public class ClaimantInformation implements CcdPageConfiguration {

    private static final String UPDATED_CLAIMANT_NAME_HINT = """
        Changing your claimant name here only updates it for this claim.
        It does not change your registered claimant name on My HMCTS.
        """;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("claimantInformation", this::midEvent)
            .pageLabel("Claimant name")
            .label("claimantInformation-separator", "---")
            .readonlyWithLabel(PCSCase::getClaimantName, "Your claimant name registered with My HMCTS is:")
            .mandatoryWithLabel(PCSCase::getIsClaimantNameCorrect,"Is this the correct claimant name?")
            .mandatory(PCSCase::getOverriddenClaimantName,
                    "isClaimantNameCorrect=\"NO\"",
                    null,
                    "What is the correct claimant name?",
                    UPDATED_CLAIMANT_NAME_HINT,
                    false);

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
        String claimantNamePossessiveForm = StringUtils.isNotEmpty(caseData.getOverriddenClaimantName())
            ? caseData.getOverriddenClaimantName()
            : caseData.getClaimantName();
        caseData.getClaimantCircumstances().setClaimantNamePossessiveForm(applyApostrophe(claimantNamePossessiveForm));
    }

    private String applyApostrophe(String value) {
        if (value == null) {
            return null;
        }
        return value.endsWith("s") || value.endsWith("S") ? value + "'" : value + "'s";
    }

}
