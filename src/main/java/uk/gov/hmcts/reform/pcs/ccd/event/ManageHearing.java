package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.DynamicMultiSelectList;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.ManageHearingOption;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.page.managehearing.ManageHearingConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.service.HearingService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;
import uk.gov.hmcts.reform.pcs.location.service.LocationReferenceService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.manageHearing;

@Component
@AllArgsConstructor
@Slf4j
public class ManageHearing implements CCDConfig<PCSCase, State, UserRole> {

    private final ManageHearingConfigurer manageHearingConfigurer;
    private final AddressFormatter addressFormatter;
    private final HearingService hearingService;
    private final LocationReferenceService locationReferenceService;
    private final PcsCaseService pcsCaseService;
    private final PartyService partyService;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder.decentralisedEvent(manageHearing.name(), this::submit, this::start)
                .forStates(State.CASE_ISSUED)
                .name("Manage hearing")
                .grant(Permission.CRUD, UserRole.HEARING_CENTRE_ADMIN)
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

        List<Integer> baseLocation = List.of(Integer.parseInt(pcsCase.getCaseManagementLocation().getBaseLocation()));

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

        if (CollectionUtils.isEmpty(pcsCase.getHearingList())) {
            pcsCase.setShowManageHearingPage(VerticalYesNo.NO);
            pcsCase.setManageHearingOption(ManageHearingOption.ADD);
        } else {
            pcsCase.setShowManageHearingPage(VerticalYesNo.YES);
        }

        return pcsCase;
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        Long caseId = eventPayload.caseReference();
        PCSCase caseData = eventPayload.caseData();
        String address = addressFormatter
            .formatMediumAddress(caseData.getPropertyAddress(), AddressFormatter.COMMA_DELIMITER);

        if (
            caseData.getManageHearingOption() == ManageHearingOption.ADD
                || caseData.getShowManageHearingPage() != VerticalYesNo.YES
        ) {
            hearingService.addHearing(caseId, caseData);
        }

        return SubmitResponse.<State>builder()
            .confirmationBody(getConfirmationBody(caseId, address, caseData.getCaseNameHmctsInternal()))
            .build();
    }

    private String getConfirmationBody(Long caseId, String address, String caseName) {
        return """
            ---
            <div class="govuk-panel govuk-panel--confirmation govuk-!-padding-top-3 govuk-!-padding-bottom-3">
            <span class="govuk-panel__title govuk-!-font-size-36">Hearing Added</span><br>
            <span class="govuk-panel__body">Case number #%s</span><br>
            <span class="govuk-panel__body">%s</span><br>
            <span class="govuk-panel__body">%s</span><br>
            </div>

            <h3>What happens next</h3>

            A hearing notice will be issued if you specified one is needed.
            """.formatted(caseId, address, caseName);
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

}
