package uk.gov.hmcts.reform.pcs.ccd.event.caseworker.addparty;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.AddPartyDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.ManagePartyOptions;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.manageparty.AddLitigationParty;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.manageparty.AddPartyDetailsPage;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.manageparty.ManagePartyOptionsPage;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.caseworker.manageparty.ManagePartyService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;

import java.util.List;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerRoles.CASEWORKER_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.manageParties;
import static uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter.COMMA_DELIMITER;

@Component
@AllArgsConstructor
public class ManageParty implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;
    private final PartyService partyService;
    private final AddressFormatter addressFormatter;
    private final ManagePartyService managePartyService;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder = configBuilder
            .decentralisedEvent(manageParties.name(), this::submit, this::start)
            .forStates(State.values())
            .name("Manage parties")
            .grant(Permission.CRUD, CASEWORKER_ROLES)
            .grantHistoryOnly(JUDICIAL_HISTORY_ROLES)
            .showSummary()
            .endButtonLabel("Submit");

        new PageBuilder(eventBuilder)
            .add(new ManagePartyOptionsPage())
            .add(new AddLitigationParty())
            .add(new AddPartyDetailsPage());
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();

        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(eventPayload.caseReference());
        ClaimEntity mainClaim = pcsCaseEntity.getClaims().getFirst();

        caseData.getAddPartyDetails().setPartyRadioList(buildApplicantPartyList(mainClaim));

        return caseData;
    }

    private DynamicList buildApplicantPartyList(ClaimEntity mainClaim) {
        List<DynamicListElement> listItems = mainClaim.getClaimParties().stream()
            .filter(claimPartyEntity -> claimPartyEntity.getRole() == PartyRole.CLAIMANT
                || claimPartyEntity.getRole() == PartyRole.DEFENDANT)
            .map(claimPartyEntity -> DynamicListElement.builder()
                .code(claimPartyEntity.getParty().getId())
                .label("%s - %s".formatted(
                    buildPartyDisplayName(claimPartyEntity.getParty()),
                    partyService.getPartyLabel(mainClaim, claimPartyEntity.getParty().getId())
                ))
                .build())
            .toList();

        return DynamicList.builder().listItems(listItems).build();
    }

    private String buildPartyDisplayName(PartyEntity partyEntity) {
        if (partyEntity.getNameKnown() == VerticalYesNo.NO) {
            return "Person unknown";
        }
        return partyService.getPartyName(partyEntity);
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {

        PCSCase caseData = eventPayload.caseData();
        AddPartyDetails partyDetails = caseData.getAddPartyDetails();
        if (partyDetails.getManagePartyOptions() != ManagePartyOptions.ADD_PARTY) {
            return SubmitResponse.<State>builder()
                .build();
        }

        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(eventPayload.caseReference());
        ClaimEntity mainClaim = pcsCaseEntity.getClaims().getFirst();
        DynamicList partyRadioList = partyDetails.getPartyRadioList();
        UUID actingForPartyId = partyRadioList != null ? partyRadioList.getValueCode() : null;
        managePartyService.addParty(partyDetails, pcsCaseEntity, mainClaim, actingForPartyId);

        return SubmitResponse.<State>builder()
            .confirmationBody(buildConfirmationPageForParty(caseData, eventPayload.caseReference()))
            .build();
    }

    private String buildConfirmationPageForParty(PCSCase pcsCase, long caseReference) {
        AddPartyDetails partyDetails = pcsCase.getAddPartyDetails();

        String partyName = switch (partyDetails.getAddPartyType()) {
            case CLAIMANT -> resolvePartyName(
                partyDetails.getClaimantOrganisationName(), partyDetails.getClaimantName());
            case DEFENDANT -> partyDetails.getFirstName() + " " + partyDetails.getLastName();
            case LITIGATION_FRIEND -> resolvePartyName(
                partyDetails.getLitigationFriendOrganisationName(), partyDetails.getLitigationFriendName());
        };

        return buildConfirmationMarkdown(
            partyName, caseReference, pcsCase.getPropertyAddress(), pcsCase.getCaseNameHmctsInternal());
    }

    private String resolvePartyName(String organisationName, String personName) {
        return StringUtils.isNotBlank(organisationName) ? organisationName : personName;
    }

    private String buildConfirmationMarkdown(String partyName, long caseReference, AddressUK address, String caseName) {
        String formatAddress = addressFormatter.formatShortAddress(address, COMMA_DELIMITER);
        return """
            ---
            <div class="govuk-panel govuk-panel--confirmation govuk-!-padding-top-3 govuk-!-padding-bottom-3">
            <span class="govuk-panel__title govuk-!-font-size-36">Party %s added</span><br>
            <span class="govuk-panel__body">Case number: %s</span><br>
            <span class="govuk-panel__body">%s</span>
            <span class="govuk-panel__body">%s</span>
            </div>

            <h3 class="govuk-heading-s">What happens next</h3>
            <p class="govuk-body govuk-!-margin-bottom-6">If the application was made without notice, only the applicant
            will be informed. Otherwise, all parties will be informed.</p>
            """.formatted(partyName,caseReference, formatAddress, caseName);
    }
}
