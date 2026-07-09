package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.ManageHearingOption;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.page.managehearing.ManageHearingConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.service.HearingService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicMultiSelectStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;
import uk.gov.hmcts.reform.pcs.location.service.LocationReferenceService;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.manageHearing;

@Component
@AllArgsConstructor
public class ManageHearing implements CCDConfig<PCSCase, State, UserRole> {

    private final ManageHearingConfigurer manageHearingConfigurer;
    private final AddressFormatter addressFormatter;
    private final HearingService hearingService;
    private final LocationReferenceService locationReferenceService;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder.decentralisedEvent(manageHearing.name(), this::submit, this::start)
                .forStates(State.PENDING_CASE_ISSUED, State.CASE_ISSUED)
                .name("Manage hearing")
                .grant(Permission.CRUD, UserRole.PCS_SOLICITOR)
                .grantHistoryOnly(JUDICIAL_HISTORY_ROLES)
                .showSummary()
                .endButtonLabel("Submit");

        manageHearingConfigurer.configurePages(new PageBuilder(eventBuilder));
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase pcsCase = eventPayload.caseData();

        List<DynamicStringListElement> listItems =
            buildPartyListItems(pcsCase.getAllClaimants(), pcsCase.getAllDefendants());
        pcsCase.setPartyMultiSelectionList(
            DynamicMultiSelectStringList.builder()
                .listItems(listItems)
                .build()
        );

        List<Integer> baseLocation = List.of(Integer.parseInt(pcsCase.getCaseManagementLocation().getBaseLocation()));
        List<CourtVenue> courtVenues = locationReferenceService.getCourtVenues(baseLocation);

        if (!CollectionUtils.isEmpty(courtVenues)) {
            CourtVenue courtVenue = courtVenues.getFirst();
            pcsCase.setHearingLocation(courtVenue.courtName());
        }

        if (CollectionUtils.isEmpty(pcsCase.getHearingList())) {
            pcsCase.setManageHearingOption(ManageHearingOption.ADD);
        } else {
            pcsCase.setShowManageHearingPage(YesOrNo.YES);
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
                || caseData.getShowManageHearingPage() != YesOrNo.YES
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

            Work allocation tasks will be created for court staff to complete on the review dates.
            """.formatted(caseId, address, caseName);
    }

    private List<DynamicStringListElement> buildPartyListItems(
        List<ListValue<Party>> claimants,
        List<ListValue<Party>> defendants
    ) {
        List<DynamicStringListElement> listItems = new ArrayList<>();

        for (int i = 0; i < claimants.size(); i++) {
            ListValue<Party> listValue = claimants.get(i);
            String partyId = listValue.getId();
            Party party = listValue.getValue();

            listItems.add(
                DynamicStringListElement.builder()
                  .code(partyId)
                  .label(formatClaimantListItemLabel(party, i))
                  .build()
            );
        }

        for (int i = 0; i < defendants.size(); i++) {
            ListValue<Party> listValue = defendants.get(i);
            String partyId = listValue.getId();
            Party party = listValue.getValue();

            listItems.add(
                DynamicStringListElement.builder()
                    .code(partyId)
                    .label(formatDefendantListItemLabel(party, i))
                    .build()
            );
        }

        return listItems;
    }

    private String formatClaimantListItemLabel(Party claimant, int index) {
        return claimant.getOrgName() + " - Claimant " + index;
    }

    private String formatDefendantListItemLabel(Party defendant, int index) {
        String name;
        if (defendant.getNameKnown() == VerticalYesNo.YES) {
            name = defendant.getFirstName() + " " + defendant.getLastName();
        } else {
            name = "Person unknown";
        }
        return name + " - Defendant " + index;
    }
}
