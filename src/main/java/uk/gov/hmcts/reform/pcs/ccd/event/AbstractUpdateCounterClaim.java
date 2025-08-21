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
import uk.gov.hmcts.reform.pcs.ccd.domain.Claim;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimType;
import uk.gov.hmcts.reform.pcs.ccd.domain.CounterClaimEvent;
import uk.gov.hmcts.reform.pcs.ccd.domain.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.page.updatecounterclaim.AddNote;
import uk.gov.hmcts.reform.pcs.ccd.page.updatecounterclaim.MakePayment;
import uk.gov.hmcts.reform.pcs.ccd.page.updatecounterclaim.RequestBreathingSpace;
import uk.gov.hmcts.reform.pcs.ccd.page.updatecounterclaim.SelectClaimAction;
import uk.gov.hmcts.reform.pcs.ccd.page.updatecounterclaim.SettleClaim;
import uk.gov.hmcts.reform.pcs.ccd.service.ClaimEventLogService;
import uk.gov.hmcts.reform.pcs.ccd.service.ClaimService;
import uk.gov.hmcts.reform.pcs.ccd.service.CounterClaimEventService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.type.poc.DynamicList;
import uk.gov.hmcts.reform.pcs.ccd.type.poc.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.roles.service.UserInfoService;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.PCS_CASE_WORKER;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.updateCounterClaim;

@Component
@Slf4j
@AllArgsConstructor
public abstract class AbstractUpdateCounterClaim implements CCDConfig<PCSCase, State, UserRole> {

    private static final int BREATHING_SPACE_SECONDS = 10;

    private final ClaimService claimService;
    private final UserInfoService userInfoService;
    private final ClaimEventLogService claimEventLogService;
    private final PcsCaseService pcsCaseService;
    private final CounterClaimEventService counterClaimEventService;

    protected abstract int getClaimIndex();

    @Override
    public void configure(final ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(updateCounterClaim.name() + getClaimIndex(), this::submit, this::start)
                .forAllStates()// TODO: Prevent for some states?
                .name("Update counter claim")
                .showCondition(ShowConditions.NEVER_SHOW)
                .grant(Permission.CRU, PCS_CASE_WORKER);


        new PageBuilder(eventBuilder)
            .add(new SelectClaimAction())
            .add(new MakePayment())
            .add(new AddNote())
            .add(new RequestBreathingSpace())
            .add(new SettleClaim());
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();
        long caseReference = eventPayload.caseReference();

        // Workaround
        UUID claimId = getClaimId(caseReference);

        caseData.setClaimId(claimId.toString());

        Claim claim = claimService.getClaim(claimId);

        if (claim.getType() != ClaimType.COUNTER_CLAIM) {
            throw new IllegalArgumentException("Claim " + claimId + " is not a counterclaim");
        }

        UserInfo userInfo = userInfoService.getCurrentUserInfo();

        // TODO: HTML escape or change label
        caseData.setClaimDescriptionMarkdown("<h3>Counterclaim: %s</h3>".formatted(claim.getSummary()));

        List<CounterClaimEvent> actionsForState = claimService.getApplicableCounterClaimEvents(claim.getState());

        List<String> userRoles = new ArrayList<>(userInfo.getRoles());
        String userEmail = userInfo.getSub();

        if (userEmail.equals(claim.getApplicantEmail())) {
            userRoles.add(CounterClaimEventService.CLAIM_APPLICANT);
        }

        if (userEmail.equals(claim.getRespondentEmail())) {
            userRoles.add(CounterClaimEventService.CLAIM_RESPONDENT);
        }

        List<CounterClaimEvent> events = filterByUserRoles(actionsForState, userRoles);
        DynamicList claimEventDynamicList = toDynamicStringList(events);
        caseData.setActionList(claimEventDynamicList);

        return caseData;
    }

    private static DynamicList toDynamicStringList(List<CounterClaimEvent> events) {
        List<DynamicStringListElement> listItems = events.stream()
            .map(value -> DynamicStringListElement.builder().code(UUID.randomUUID().toString())
                .label(value.getLabel()).build())
            .toList();

        return DynamicList.builder()
            .listItems(listItems)
            .build();
    }

    // Workaround for CCD not passing event params at the moment
    private UUID getClaimId(long caseReference) {
        List<UUID> counterClaimIds = pcsCaseService.getCaseByCaseReference(caseReference)
            .getClaims()
            .stream()
            .filter(claimEntity -> ClaimType.COUNTER_CLAIM.equals(claimEntity.getType()))
            .map(ClaimEntity::getId)
            .toList();

        return counterClaimIds.get(getClaimIndex());
    }

    private List<CounterClaimEvent> filterByUserRoles(List<CounterClaimEvent> events, List<String> userRoles) {
        if (userRoles == null || userRoles.isEmpty()) {
            return List.of();
        }

        return events.stream()
            .filter(event -> event.getApplicableRoles().stream().anyMatch(userRoles::contains))
            .toList();
    }

    private void submit(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();

        UUID claimId = UUID.fromString(caseData.getClaimId());
        String selectedActionLabel = caseData.getSelectedAction();
        CounterClaimEvent counterClaimEvent = counterClaimEventService.getEventByLabel(selectedActionLabel);

        if (counterClaimEvent.getId().equals("REQUEST_BREATHING_SPACE")) {
            startBreathingSpaceTimer(claimId);
        }

        String logSummary = "";
        CounterClaimState endState = counterClaimEvent.getEndState();
        if (endState != null) {
            log.info("Setting state to " + endState);
            claimService.setClaimState(claimId, endState);
            logSummary = "Claim state updated to %s".formatted(endState);
        }

        claimEventLogService.writeEntry(claimId, counterClaimEvent, logSummary);
    }


    private void startBreathingSpaceTimer(UUID claimId) {
        TimerTask task = new TimerTask() {
            public void run() {
                System.out.println("Exiting breathing space");
                claimService.setClaimState(claimId, CounterClaimState.CLAIM_ISSUED);
            }
        };
        Timer timer = new Timer("breathing-space-timer");

        timer.schedule(task, BREATHING_SPACE_SECONDS * 1000L);
    }

}
