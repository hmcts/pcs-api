package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrantofrestitution;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

import static uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType.WARRANT_OF_RESTITUTION_FLOW;

public class EvictionDelayWarningPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("warrantOfRestitutionEvictionDelayWarning")
            .pageLabel("The eviction could be delayed if the bailiff identifies a risk on the day")
            .showCondition(
                WARRANT_OF_RESTITUTION_FLOW
                    + " AND (warrant_restAnyRiskToBailiff=\"NO\" "
                    + "OR warrant_restAnyRiskToBailiff=\"NOT_SURE\")"
            )
            .label("warrantOfRestitutionEvictionDelayWarning-line-separator", "---")
            .label(
                "warrantOfRestitutionEvictionDelayWarning-text",
                """
                      <div class="govuk-warning-text">
                      <span class="govuk-warning-text__icon" aria-hidden="true">!</span>
                      <strong class="govuk-warning-text__text">
                        <span class="govuk-visually-hidden">Warning</span>
                            The bailiff may not be able to carry out the eviction if they identify a risk on the
                            eviction day
                        </strong>
                      </div>
                      <p class="govuk-body">For example, if the bailiffs arrive to carry out the eviction and they
                      discover a dangerous dog on the premises.</p>"""
            );
    }
}

