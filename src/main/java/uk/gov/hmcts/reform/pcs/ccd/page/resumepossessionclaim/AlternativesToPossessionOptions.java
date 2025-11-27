package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.AlternativesToPossession;
import uk.gov.hmcts.reform.pcs.ccd.domain.DemotionOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuy;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuyDemotionOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

@AllArgsConstructor
@Component
public class AlternativesToPossessionOptions implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("alternativesToPossession", this::midEvent)
            .pageLabel("Alternatives to possession")
            .showCondition("legislativeCountry!=\"Wales\"")
            .complex(PCSCase::getSuspensionOfRightToBuy)
            .readonlyNoSummary(SuspensionOfRightToBuy::getShowSuspensionOfRightToBuyHousingActsPage,NEVER_SHOW)
            .done()
            .complex(PCSCase::getDemotionOfTenancy)
            .readonlyNoSummary(DemotionOfTenancy::getShowDemotionOfTenancyHousingActsPage,NEVER_SHOW)
            .done()
            .complex(PCSCase::getSuspensionOfRightToBuyDemotionOfTenancy)
            .readonlyNoSummary(SuspensionOfRightToBuyDemotionOfTenancy::getSuspensionToBuyDemotionOfTenancyPages,
                               NEVER_SHOW)
            .done()
            .label("alternativesToPossession-info", """
                    ---
                    <p class="govuk-body govuk-!-margin-bottom-1" tabindex="0">
                      If a judge decides that possession is not reasonable at this time, they may instead decide
                      to order a demotion of tenancy (demotion order) or a suspension of the defendants’ right
                      to buy (suspension order), if they’re not already in place.
                    </p>

                    <h2 class="govuk-heading-l govuk-!-margin-top-1" tabindex="0">Suspension of right to buy</h2>

                    <p class="govuk-body govuk-!-margin-bottom-1" tabindex="0">
                      A suspension order means that the defendants will not have a right to buy the premises
                      during the suspension.
                    </p>

                    <h2 class="govuk-heading-l govuk-!-margin-top-1" tabindex="0">Demotion of tenancy</h2>

                    <p class="govuk-body" tabindex="0">
                      A demotion order means that the defendants’ current tenancy will be replaced with a
                      demoted tenancy. During this period (usually 12 months) they will lose some rights they
                      currently have. The claimant is able to propose a new set of terms that will be put in
                      place by the demotion order.
                    </p>
                    """)
            .optional(PCSCase::getAlternativesToPossession)
            .label("alternativesToPossession-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {

        PCSCase caseData = details.getData();

        Set<AlternativesToPossession> altToPossessions = caseData.getAlternativesToPossession();

        boolean showSuspensionPage = altToPossessions.contains(AlternativesToPossession.SUSPENSION_OF_RIGHT_TO_BUY)
            && !altToPossessions.contains(AlternativesToPossession.DEMOTION_OF_TENANCY);

        boolean showDemotionPage = altToPossessions.contains(AlternativesToPossession.DEMOTION_OF_TENANCY)
            && !altToPossessions.contains(AlternativesToPossession.SUSPENSION_OF_RIGHT_TO_BUY);

        boolean showSuspensionAndDemotionPage = altToPossessions.contains(AlternativesToPossession.DEMOTION_OF_TENANCY)
            && altToPossessions.contains(AlternativesToPossession.SUSPENSION_OF_RIGHT_TO_BUY);

        if (caseData.getSuspensionOfRightToBuy() != null) {
            caseData.getSuspensionOfRightToBuy()
                .setShowSuspensionOfRightToBuyHousingActsPage(YesOrNo.from(showSuspensionPage));
        }
        if (caseData.getDemotionOfTenancy() != null) {
            caseData.getDemotionOfTenancy()
                .setShowDemotionOfTenancyHousingActsPage(YesOrNo.from(showDemotionPage));
        }

        if (caseData.getSuspensionOfRightToBuyDemotionOfTenancy() != null) {
            caseData.getSuspensionOfRightToBuyDemotionOfTenancy()
                .setSuspensionToBuyDemotionOfTenancyPages(YesOrNo.from(showSuspensionAndDemotionPage));
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }
}
