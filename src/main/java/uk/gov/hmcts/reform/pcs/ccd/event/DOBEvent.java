
package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.DefendantsDOBMultiLabelPage;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.DefendantsDOBPage;

import static uk.gov.hmcts.reform.pcs.ccd.domain.State.AWAITING_SUBMISSION_TO_HMCTS;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.CASE_ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.dobEvent;


@Slf4j
@Component
@AllArgsConstructor
public class DOBEvent implements CCDConfig<PCSCase, State, UserRole> {

    //Attempt 1 - String interpolation and read only fields
    private final DefendantsDOBPage defendantsDOBPage;
    //Attempt 2 - Prepopulated ui.
    private final DefendantsDOBMultiLabelPage defendantsDOBConceptsPage;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(dobEvent.name(), this::submit, this::start)
                .forStateTransition(AWAITING_SUBMISSION_TO_HMCTS, CASE_ISSUED)
                .name("Enforce the order")
                .grant(Permission.CRUD, UserRole.PCS_SOLICITOR);

        new PageBuilder(eventBuilder)
            .add(defendantsDOBPage)
            .add(defendantsDOBConceptsPage);
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();
        return caseData;
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        PCSCase pcsCase = eventPayload.caseData();
        return SubmitResponse.defaultResponse();
    }
}

