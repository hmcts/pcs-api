package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrantofrestitution;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

import static uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType.WARRANT_OF_RESTITUTION_FLOW;

public class LivingInThePropertyIntroPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("warrantOfRestitutionAnyoneAtPropertyRiskIntro")
            .pageLabel("On the next few questions, we will ask you to tell the bailiff if anyone at the property still "
                       + "poses a risk")
            .showCondition(WARRANT_OF_RESTITUTION_FLOW)
            .label("warrantOfRestitutionAnyoneAtPropertyRiskIntro-line-separator", "---")
            .label(
                "warrantOfRestitutionAnyoneAtPropertyRiskIntro-text",
                """
                    <p class="govuk-body govuk-!-margin-bottom-1">For example, we will ask you to:</p>
                    <ul class="govuk-list govuk-list--bullet">
                        <li class="govuk-!-font-size-19">check your previous answers to the bailiff’s questions</li>
                        <li class="govuk-!-font-size-19">update your answers if something has changed</li>
                    </ul>
                    <p class="govuk-body">
                        The bailiff will use this information to prepare for the eviction.
                    </p>
                    """
            );
    }
}

