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
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.StartTheService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import java.math.BigDecimal;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.createTestCase;

@Component
@Slf4j
@AllArgsConstructor
public class CaseCreationTestingSupport implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;


    private static final String FEE = "Unable to retrieve";

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(createTestCase.name(), this::submit, this::start)
                .initialState(State.TEST_CASE)
                .name("FOR QA - Test Case Creation")
                .grant(Permission.CRUD, UserRole.PCS_SOLICITOR);

        new PageBuilder(eventBuilder)
            .add(new StartTheService());

    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();


        return caseData;
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase caseData = eventPayload.caseData();

        pcsCaseService.createCase(caseReference, caseData.getPropertyAddress(), caseData.getLegislativeCountry());

        return SubmitResponse.defaultResponse();
    }

    private String formatAsCurrency(BigDecimal amount) {
        if (amount == null) {
            return FEE;
        }
        return "£" + amount.stripTrailingZeros().toPlainString();
    }

}
