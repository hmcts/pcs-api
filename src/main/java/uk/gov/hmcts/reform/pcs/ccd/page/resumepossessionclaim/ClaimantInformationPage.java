package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.PossessiveNameService;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

@Component
@AllArgsConstructor
public class ClaimantInformationPage implements CcdPageConfiguration {

    private static final String ORG_NAME_FOUND = "orgNameFound=\"Yes\"";
    private static final String ORG_NAME_NOT_FOUND = "orgNameFound=\"No\"";

    private final PossessiveNameService possessiveNameService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("claimantInformation", this::midEvent)
            .pageLabel("Claimant name")
            .label("claimantInformation-separator", "---")
            .complex(PCSCase::getClaimantInformation)
            .readonly(ClaimantInformation::getOrgNameFound, NEVER_SHOW, true)
            .readonlyNoSummary(ClaimantInformation::getClaimantName, ORG_NAME_FOUND)
            .mandatory(ClaimantInformation::getIsClaimantNameCorrect, ORG_NAME_FOUND)
            .label("claimantInformation-name-missing", """
                    <h3 class="govuk-heading-m govuk-!-margin-bottom-1">
                        We could not retrieve the claimant name linked to your My HMCTS account
                    </h3>
                    <p class="govuk-hint govuk-!-margin-top-1">
                        You must enter the claimant name you’d like to use for this claim
                    </p>
                    """, ORG_NAME_NOT_FOUND)
            .mandatory(
                ClaimantInformation::getOverriddenClaimantName,
                "isClaimantNameCorrect=\"NO\""
            )
            .mandatory(
                ClaimantInformation::getFallbackClaimantName,
                ORG_NAME_NOT_FOUND
            )
            .done()
            .label("claimantInformation-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);

    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        setClaimantNamePossessiveForm(caseData);

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }

    private void setClaimantNamePossessiveForm(PCSCase caseData) {
        ClaimantInformation claimantInfo = caseData.getClaimantInformation();

        String claimantName;
        if (claimantInfo.getOrgNameFound() == YesOrNo.NO) {
            claimantName = claimantInfo.getFallbackClaimantName();
        } else if (claimantInfo.getIsClaimantNameCorrect() == VerticalYesNo.NO) {
            claimantName = claimantInfo.getOverriddenClaimantName();
        } else {
            claimantName = claimantInfo.getClaimantName();
        }

        caseData.getClaimantCircumstances()
            .setClaimantNamePossessiveForm(possessiveNameService.applyApostrophe(claimantName));
    }


}
