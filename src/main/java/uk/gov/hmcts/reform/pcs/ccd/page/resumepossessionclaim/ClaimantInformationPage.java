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

import static uk.gov.hmcts.ccd.sdk.api.ShowCondition.NEVER_SHOW;
import static uk.gov.hmcts.ccd.sdk.api.ShowCondition.when;
import static uk.gov.hmcts.ccd.sdk.type.YesOrNo.NO;
import static uk.gov.hmcts.ccd.sdk.type.YesOrNo.YES;

@Component
@AllArgsConstructor
public class ClaimantInformationPage implements CcdPageConfiguration {

    private final PossessiveNameService possessiveNameService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("claimantInformation", this::midEvent)
            .pageLabel("Claimant name")
            .label("claimantInformation-separator", "---")
            .complex(PCSCase::getClaimantInformation)
            .readonly(ClaimantInformation::getOrgNameFound, NEVER_SHOW, true)
            .readonlyNoSummaryWhen(ClaimantInformation::getClaimantName,
                when(ClaimantInformation::getOrgNameFound).is(YES))
            .mandatoryWhen(ClaimantInformation::getIsClaimantNameCorrect,
                when(ClaimantInformation::getOrgNameFound).is(YES))
            .labelWhen("claimantInformation-name-missing", """
                    <h3 class="govuk-heading-m govuk-!-margin-bottom-1">
                        We could not retrieve the claimant name linked to your My HMCTS account
                    </h3>
                    <p class="govuk-hint govuk-!-margin-top-1">
                        You must enter the claimant name you’d like to use for this claim
                    </p>
                    """, when(ClaimantInformation::getOrgNameFound).is(NO))
            .mandatoryWhen(
                ClaimantInformation::getOverriddenClaimantName,
                when(ClaimantInformation::getIsClaimantNameCorrect).is(VerticalYesNo.NO)
            )
            .mandatoryWhen(
                ClaimantInformation::getFallbackClaimantName,
                when(ClaimantInformation::getOrgNameFound).is(NO)
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
