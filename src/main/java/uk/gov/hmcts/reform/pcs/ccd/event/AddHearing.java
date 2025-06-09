package uk.gov.hmcts.reform.pcs.ccd.event;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.domain.PcsCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;

@Component
public class AddHearing implements CCDConfig<PcsCase, State, UserRole> {

    @Override
    public void configure(ConfigBuilder<PcsCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent("hearing", this::submit)
            .forAllStates()
            .name("Add Hearing")
            .showSummary()
            .grant(Permission.CRUD, UserRole.CASE_WORKER)
            .fields()
            .page("hearingDetails")
            .mandatory(PcsCase::getHearingDate)
            .mandatory(PcsCase::getHearingLocation)
            .done();
    }

    private void submit(EventPayload<PcsCase, State> eventPayload) {
    }
}
