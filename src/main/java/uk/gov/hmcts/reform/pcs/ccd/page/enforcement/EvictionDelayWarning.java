package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;


/**
 * Full implementation will be done in another ticket - responses not captured at the moment.
 */
@AllArgsConstructor
@Component
public class EvictionDelayWarning implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("evictionDelayWarning")
            .pageLabel("The eviction could be delayed if the bailiff identifies a risk on the day")
            .showCondition("confirmLivingAtProperty=\"NOT_SURE\"")
            .label("evictionDelayWarning-line-separator", "---")
            .label(
                "evictionDelayText",
                """
                      <div class="govuk-warning-text">
                      <span class="govuk-warning-text__icon" aria-hidden="true">!</span>
                      <strong class="govuk-warning-text__text">
                        <span class="govuk-visually-hidden">Warning</span>
                            The bailiff may not be able to carry out the eviction if they identify a risk on the eviction day
                        </strong>
                      </div>
                      <p class=\"govuk-body\"> For example, if the bailiffs arrive to carry out the eviction and they discover a dangerous dog on the premises.</p>""");
    }

}
