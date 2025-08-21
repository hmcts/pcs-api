package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.GenApp;
import uk.gov.hmcts.reform.pcs.ccd.domain.GenAppEvent;
import uk.gov.hmcts.reform.pcs.ccd.domain.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.page.updategenapp.AcceptGenApp;
import uk.gov.hmcts.reform.pcs.ccd.page.updategenapp.MakePayment;
import uk.gov.hmcts.reform.pcs.ccd.page.updategenapp.RejectGenApp;
import uk.gov.hmcts.reform.pcs.ccd.page.updategenapp.SelectAction;
import uk.gov.hmcts.reform.pcs.ccd.service.GenAppEventLogService;
import uk.gov.hmcts.reform.pcs.ccd.service.GenAppEventService;
import uk.gov.hmcts.reform.pcs.ccd.service.GenAppService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.type.poc.DynamicList;
import uk.gov.hmcts.reform.pcs.ccd.type.poc.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.roles.service.UserInfoService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.PCS_CASE_WORKER;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.updateGenApp;

@Component
@Slf4j
@AllArgsConstructor
public abstract class AbstractUpdateGenApp implements CCDConfig<PCSCase, State, UserRole> {

    private final GenAppService genAppService;
    private final UserInfoService userInfoService;
    private final GenAppEventLogService genAppEventLogService;
    private final PcsCaseService pcsCaseService;
    private final GenAppEventService genAppEventService;

    protected abstract int getGenAppIndex();

    @Override
    public void configure(final ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(updateGenApp.name() + getGenAppIndex(), this::submit, this::start)
                .forAllStates()// TODO: Prevent for some states?
                .name("Update general application")
                .showCondition(ShowConditions.NEVER_SHOW)
                .grant(Permission.CRU, PCS_CASE_WORKER);


        new PageBuilder(eventBuilder)
            .add(new SelectAction())
            .add(new MakePayment())
            .add(new AcceptGenApp())
            .add(new RejectGenApp());
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();
        long caseReference = eventPayload.caseReference();

        // Workaround
        UUID genAppId = getGenAppId(caseReference);

        caseData.setGenAppId(genAppId.toString());

        GenApp genApp = genAppService.getGenApp(genAppId);

        UserInfo userInfo = userInfoService.getCurrentUserInfo();

        // TODO: HTML escape or change label
        caseData.setClaimDescriptionMarkdown("<h3>General Application: %s</h3>".formatted(genApp.getSummary()));

        List<GenAppEvent> actionsForState = genAppService.getApplicableEvents(genApp.getState());

        List<String> userRoles = new ArrayList<>(userInfo.getRoles());

        List<GenAppEvent> events = filterByUserRoles(actionsForState, userRoles);
        DynamicList claimEventDynamicList = toDynamicStringList(events);
        caseData.setActionList(claimEventDynamicList);

        return caseData;
    }

    private static DynamicList toDynamicStringList(List<GenAppEvent> events) {
        List<DynamicStringListElement> listItems = events.stream()
            .map(value -> DynamicStringListElement.builder().code(UUID.randomUUID().toString())
                .label(value.getLabel()).build())
            .toList();

        return DynamicList.builder()
            .listItems(listItems)
            .build();
    }

    // Workaround for CCD not passing event params at the moment
    private UUID getGenAppId(long caseReference) {
        List<UUID> genAppIds = pcsCaseService.getCaseByCaseReference(caseReference)
            .getGenApps()
            .stream()
            .map(GenAppEntity::getId)
            .toList();

        return genAppIds.get(getGenAppIndex());
    }

    private List<GenAppEvent> filterByUserRoles(List<GenAppEvent> events, List<String> userRoles) {
        if (userRoles == null || userRoles.isEmpty()) {
            return List.of();
        }

        return events.stream()
            .filter(event -> event.getApplicableRoles().stream().anyMatch(userRoles::contains))
            .toList();
    }

    private void submit(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();

        UUID genAppId = UUID.fromString(caseData.getGenAppId());
        String selectedActionLabel = caseData.getSelectedAction();
        GenAppEvent genAppEvent = genAppEventService.getEventByLabel(selectedActionLabel);

        String logSummary = "";
        GenAppState endState = genAppEvent.getEndState();
        if (endState != null) {
            log.info("Setting state to " + endState);
            genAppService.setGenAppState(genAppId, endState);
            logSummary = "GenApp state updated to %s".formatted(endState);
        }

        genAppEventLogService.writeEntry(genAppId, genAppEvent, logSummary);
    }



}
