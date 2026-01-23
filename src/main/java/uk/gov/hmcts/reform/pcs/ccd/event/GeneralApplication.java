package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ChooseGeneralApplicationPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.GenApplicationPlaceholder;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.generalApplication;

@Slf4j
@Component
@AllArgsConstructor
public class GeneralApplication implements CCDConfig<PCSCase, State, UserRole> {

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
                configBuilder
                        .decentralisedEvent(generalApplication.name(), this::submit, this::start)
                        .forAllStates()
                        .name("Make a Gen Application")
                        .grant(Permission.CRUD, UserRole.PCS_SOLICITOR)
                        .showSummary();

        new PageBuilder(eventBuilder)
            .add(new ChooseGeneralApplicationPage())
            .add(new GenApplicationPlaceholder());
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        return eventPayload.caseData();
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        return SubmitResponse.defaultResponse();
    }
}
