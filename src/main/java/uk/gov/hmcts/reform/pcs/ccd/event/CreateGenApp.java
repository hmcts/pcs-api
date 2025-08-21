package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.GenAppEvent;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.GenAppEventLogService;
import uk.gov.hmcts.reform.pcs.ccd.service.GenAppEventService;
import uk.gov.hmcts.reform.pcs.ccd.service.GenAppService;

import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.PCS_CASE_WORKER;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.CASE_ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.createGenApp;

@Component
@Slf4j
@AllArgsConstructor
public class CreateGenApp implements CCDConfig<PCSCase, State, UserRole> {

    private final GenAppService genAppService;
    private final GenAppEventLogService genAppEventLogService;
    private final GenAppEventService genAppEventService;

    @Override
    public void configure(final ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(createGenApp.name(), this::submit)
            .forStates(CASE_ISSUED)
            .name("Create general application")
            .grant(Permission.CRU, PCS_CASE_WORKER)
            .fields()
            .page("page-1")
            .pageLabel("Provide additional details")
                .mandatory(PCSCase::getGenAppDetails)
            .done();
    }

    private void submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase caseData = eventPayload.caseData();

        UUID claimId = genAppService.createGenApp(caseReference, caseData.getGenAppDetails());

        // Pseudo claim event just for history purposes
        GenAppEvent genAppEventServiceEventByLabel = genAppEventService.getEventByLabel("Create general application");

        genAppEventLogService.writeEntry(claimId, genAppEventServiceEventByLabel, "");
    }

}
