package uk.gov.hmcts.reform.pcs.ccd.event.hearing;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.CaseLocation;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.DynamicMultiSelectList;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.ManageHearingOption;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.HearingEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.page.managehearing.ManageHearingConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.hearing.HearingService;
import uk.gov.hmcts.reform.pcs.ccd.service.hearing.HearingSummaryRenderer;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;
import uk.gov.hmcts.reform.pcs.location.service.LocationReferenceService;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerRoles.CASEWORKER_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.manageHearing;

@Component
@Slf4j
public class ManageHearing implements CCDConfig<PCSCase, State, UserRole> {

    private final ManageHearingConfigurer manageHearingConfigurer;
    private final HearingService hearingService;
    private final LocationReferenceService locationReferenceService;
    private final PcsCaseService pcsCaseService;
    private final PartyService partyService;
    private final HearingSummaryRenderer hearingSummaryRenderer;
    private final ConfirmationBodyRenderer confirmationBodyRenderer;
    private final Clock ukClock;

    public ManageHearing(ManageHearingConfigurer manageHearingConfigurer,
                         HearingService hearingService,
                         LocationReferenceService locationReferenceService,
                         PcsCaseService pcsCaseService,
                         PartyService partyService,
                         HearingSummaryRenderer hearingSummaryRenderer,
                         ConfirmationBodyRenderer confirmationBodyRenderer,
                         @Qualifier("ukClock") Clock ukClock) {

        this.manageHearingConfigurer = manageHearingConfigurer;
        this.hearingService = hearingService;
        this.locationReferenceService = locationReferenceService;
        this.pcsCaseService = pcsCaseService;
        this.partyService = partyService;
        this.hearingSummaryRenderer = hearingSummaryRenderer;
        this.confirmationBodyRenderer = confirmationBodyRenderer;
        this.ukClock = ukClock;
    }

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder.decentralisedEvent(manageHearing.name(), this::submit, this::start)
                .forStates(State.CASE_ISSUED)
                .name("Manage hearing")
                .grant(Permission.CRUD, CASEWORKER_ROLES)
                .grantHistoryOnly(JUDICIAL_HISTORY_ROLES)
                .showSummary()
                .endButtonLabel("Submit");

        manageHearingConfigurer.configurePages(new PageBuilder(eventBuilder));
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase pcsCase = eventPayload.caseData();

        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);

        pcsCase.setPartyMultiSelectionList(buildPartyList(pcsCaseEntity));

        setHearingLocation(eventPayload, pcsCase);

        List<HearingEntity> futureHearings = getFutureHearings(pcsCaseEntity);

        if (futureHearings.isEmpty()) {
            pcsCase.setShowManageHearingPage(VerticalYesNo.NO);
            pcsCase.setManageHearingOption(ManageHearingOption.ADD);
        } else {
            HearingEntity nextHearingEntity = futureHearings.getFirst();
            String hearingLocation = pcsCase.getHearingLocation();
            pcsCase.getHearing().setHearingId(nextHearingEntity.getId());
            pcsCase.getHearing().setHearingSummaryMarkdown(
                hearingSummaryRenderer.renderMarkdown(nextHearingEntity, hearingLocation));

            pcsCase.setShowManageHearingPage(VerticalYesNo.YES);
        }

        return pcsCase;
    }

    private void setHearingLocation(EventPayload<PCSCase, State> eventPayload, PCSCase pcsCase) {
        CaseLocation caseManagementLocation = pcsCase.getCaseManagementLocation();

        if (caseManagementLocation == null) {
            log.warn("Unable to find hearing location for case {}:", eventPayload.caseReference());
            pcsCase.setHearingLocation("Unable to find hearing location");
            return;
        }

        List<Integer> baseLocation = List.of(Integer.parseInt(caseManagementLocation.getBaseLocation()));

        try {
            List<CourtVenue> courtVenues = locationReferenceService.getCourtVenues(baseLocation);

            if (!CollectionUtils.isEmpty(courtVenues)) {
                CourtVenue courtVenue = courtVenues.getFirst();
                pcsCase.setHearingLocation(courtVenue.courtName());
            } else {
                log.warn("Unable to find hearing location for case {}:", eventPayload.caseReference());
                pcsCase.setHearingLocation("Unable to find hearing location");
            }
        } catch (Exception e) {
            log.warn("Unable to fetch hearing location for case {}:", eventPayload.caseReference(), e);
            pcsCase.setHearingLocation("Unable to find hearing location");
        }
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase caseData = eventPayload.caseData();

        // Default action is ADD if the choice screen wasn't shown
        if (caseData.getShowManageHearingPage() != VerticalYesNo.YES) {
            caseData.setManageHearingOption(ManageHearingOption.ADD);
        }

        String confirmationBody = "";
        switch (caseData.getManageHearingOption()) {
            case ADD: {
                hearingService.addHearing(caseReference, caseData);
                confirmationBody = confirmationBodyRenderer
                    .renderHearingAddedConfirmationBody(caseData, caseReference);
                break;
            }
            case EDIT: {
                break;
            }
            case CANCEL: {
                hearingService.cancelHearing(caseData.getHearing());
                confirmationBody = confirmationBodyRenderer
                    .renderHearingCancelledConfirmationBody(caseData, caseReference);
                break;
            }
        }

        return SubmitResponse.<State>builder()
            .confirmationBody(confirmationBody)
            .build();
    }

    private DynamicMultiSelectList buildPartyList(PcsCaseEntity pcsCaseEntity) {
        ClaimEntity mainClaim = pcsCaseEntity.getMainClaim();
        Map<PartyRole, List<ClaimPartyEntity>> partyRoleListMap = mainClaim.getClaimParties().stream()
            .collect(Collectors.groupingBy(ClaimPartyEntity::getRole));

        List<DynamicListElement> partyElementList = new ArrayList<>();

        partyRoleListMap.getOrDefault(PartyRole.CLAIMANT, List.of()).stream()
            .map(claimPartyEntity -> mapToPartyListElement(mainClaim, claimPartyEntity.getParty()))
            .forEach(partyElementList::add);

        partyRoleListMap.getOrDefault(PartyRole.DEFENDANT, List.of()).stream()
            .map(claimPartyEntity -> mapToPartyListElement(mainClaim, claimPartyEntity.getParty()))
            .forEach(partyElementList::add);

        return DynamicMultiSelectList.builder().listItems(partyElementList).build();
    }

    private DynamicListElement mapToPartyListElement(ClaimEntity mainClaim, PartyEntity partyEntity) {
        String partyName = partyService.getPartyName(partyEntity);
        String partyLabel = partyService.getPartyLabel(mainClaim, partyEntity.getId());
        String label = ("%s - %s").formatted(partyName, partyLabel);
        return DynamicListElement.builder()
            .code(partyEntity.getId())
            .label(label)
            .build();
    }

    private List<HearingEntity> getFutureHearings(PcsCaseEntity pcsCaseEntity) {
        LocalDateTime now = LocalDateTime.now(ukClock);
        return pcsCaseEntity.getHearings().stream()
            .filter(hearingEntity -> hearingEntity.getHearingDate().isAfter(now))
            .filter(hearingEntity -> BooleanUtils.isNotTrue(hearingEntity.getCancelled()))
            .sorted(Comparator.comparing(HearingEntity::getHearingDate).reversed())
            .toList();
    }

}
