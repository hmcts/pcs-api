package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import java.util.Set;


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
                    <p class=\"govuk-body\"><strong>The bailiff may not be able to carry out the eviction if they identify a risk on the eviction day</strong></p>
                    <p class=\"govuk-body\"> For example, if the bailiffs arrive to carry out the eviction and they discover a dangerous dog on the premises.</p>""");
    }

}
