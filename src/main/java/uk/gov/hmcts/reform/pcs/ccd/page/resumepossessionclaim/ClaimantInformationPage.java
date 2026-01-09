package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

@Slf4j
public class ClaimantInformationPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("claimantInformation", this::midEvent)
            .pageLabel("Claimant name")
            .label("claimantInformation-separator", "---")
            .complex(PCSCase::getClaimantInformation)
            .readonlyNoSummary(ClaimantInformation::getClaimantName)
            .mandatory(ClaimantInformation::getIsClaimantNameCorrect)
            .mandatory(
                ClaimantInformation::getOverriddenClaimantName,
                "isClaimantNameCorrect=\"NO\""
            )
            .done()
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
        ClaimantInformation claimantInfo = caseData.getClaimantInformation();
        String claimantNamePossessiveForm =
            StringUtils.isNotEmpty(claimantInfo.getOverriddenClaimantName())
            ? claimantInfo.getOverriddenClaimantName()
            : claimantInfo.getClaimantName();
        caseData.getClaimantCircumstances().setClaimantNamePossessiveForm(applyApostrophe(claimantNamePossessiveForm));
    }

    private String applyApostrophe(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        if (trimmed.isEmpty()) {
            return "";
        }

        if (trimmed.endsWith("’") || trimmed.endsWith("’s") || trimmed.endsWith("’S")) {
            return trimmed;
        }

        return trimmed.endsWith("s") || trimmed.endsWith("S") ? trimmed + "’" : trimmed + "’s";
    }

}
