package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.domain.PcsCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.hiddenEvent;

/**
 * Any user role that is not referenced in an Event permission grant will not be automatically
 * given Read access to fields on a tab in the case summary, even though they should have permission.
 * This is due to the way the CCD config generator determines the tab permissions.
 * See https://github.com/hmcts/dtsse-ccd-config-generator/issues/533
 */
@Component
@AllArgsConstructor
public class HiddenEvent implements CCDConfig<PcsCase, State, UserRole> {

    @Override
    public void configure(ConfigBuilder<PcsCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(hiddenEvent.name(), this::submit)
            .initialState(State.Open)
            .showCondition(ShowConditions.NEVER_SHOW)
            .name("Hidden Event")
            .grant(Permission.R, UserRole.values())
            .fields()
            .page("Hidden event page")
                .mandatory(PcsCase::getCaseDescription)
                .done();
    }

    public void submit(EventPayload<PcsCase, State> p) {
    }
}
