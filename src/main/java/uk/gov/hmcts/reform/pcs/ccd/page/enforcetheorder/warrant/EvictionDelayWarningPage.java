package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;

import static uk.gov.hmcts.ccd.sdk.api.ShowCondition.when;
import static uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType.WARRANT_FLOW;

public class EvictionDelayWarningPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("evictionDelayWarning")
            .pageLabel("The eviction could be delayed if the bailiff identifies a risk on the day")
            .showWhen(WARRANT_FLOW.and(when(EnforcementOrder::getWarrantDetails,
                WarrantDetails::getAnyRiskToBailiff).is(YesNoNotSure.NOT_SURE)))
            .label("evictionDelayWarning-line-separator", "---")
            .label(
                "evictionDelayWarning-text",
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
                      discover a dangerous dog on the premises.</p>""");
    }

}
