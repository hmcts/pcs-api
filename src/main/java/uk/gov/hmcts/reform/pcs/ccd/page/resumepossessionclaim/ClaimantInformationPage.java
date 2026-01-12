package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import io.micrometer.common.util.StringUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.TextValidationService;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class ClaimantInformationPage implements CcdPageConfiguration {

    private final TextValidationService textValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("claimantInformation", this::midEvent)
            .pageLabel("Claimant name")
            .label("claimantInformation-separator", "---")
            .complex(PCSCase::getClaimantInformation)
            .readonlyNoSummary(ClaimantInformation::getOrganisationName)
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
        List<String> validationErrors = new ArrayList<>();

        ClaimantInformation claimantInfo = caseData.getClaimantInformation();
        if (claimantInfo != null
            && claimantInfo.getIsClaimantNameCorrect() == VerticalYesNo.NO) {

            validationErrors.addAll(
                textValidationService.validateSingleField(
                    claimantInfo,
                    ClaimantInformation::getOverriddenClaimantName,
                    "Overridden claimant name",
                    TextValidationService.TEXT_FIELD_LIMIT
                )
            );
        }

        return textValidationService.createValidationResponse(caseData, validationErrors);
    }

    private void setClaimantNamePossessiveForm(CaseDetails<PCSCase, State> details) {
        PCSCase caseData = details.getData();
        ClaimantInformation claimantInfo = caseData.getClaimantInformation();
        String claimantNamePossessiveForm =
            StringUtils.isNotEmpty(claimantInfo.getOverriddenClaimantName())
            ? claimantInfo.getOverriddenClaimantName()
            : (StringUtils.isNotEmpty(claimantInfo.getOrganisationName())
                ? claimantInfo.getOrganisationName()
                : claimantInfo.getClaimantName());
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
