package uk.gov.hmcts.reform.pcs.ccd.event.caseworker.defendantpaperresponse;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.ContactPreferencesSelection;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoPreferNotToSay;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.DefendantPaperResponseRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantContactDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.defendantpaperresponse.ContactPreferences;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.defendantpaperresponse.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.defendantpaperresponse.DisputingOtherParts;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.defendantpaperresponse.FreeLegalAdvice;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.defendantpaperresponse.SelectDefendant;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.ClaimResponseService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.DefendantResponseService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.model.JourneyType;

import java.util.List;
import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerRoles.CASEWORKER_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.defendantPaperResponse;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.CASEWORKER_EVENTS;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.RELEASE_1_DOT_3;

@AllArgsConstructor
@Component
public class DefendantPaperResponse implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;
    private final PartyService partyService;
    private final ClaimResponseService claimResponseService;
    private final DefendantResponseService defendantResponseService;
    private final AddressMapper addressMapper;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder = configBuilder
            .decentralisedEvent(defendantPaperResponse.name(), this::submit, this::start)
            .forStates(State.CASE_ISSUED)
            .name("Paper response - Defence")
            .showCondition(ShowConditions.featureFlagsEnabled(RELEASE_1_DOT_3, CASEWORKER_EVENTS))
            .grant(Permission.CRU, CASEWORKER_ROLES)
            .grantHistoryOnly(JUDICIAL_HISTORY_ROLES)
            .endButtonLabel("Submit response")
            .showSummary();

        new PageBuilder(eventBuilder)
            .add(new SelectDefendant())
            .add(new FreeLegalAdvice())
            .add(new DefendantDetails())
            .add(new ContactPreferences())
            .add(new DisputingOtherParts());
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();

        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(eventPayload.caseReference());
        ClaimEntity claimEntity = pcsCaseEntity.getClaims().getFirst();

        caseData.setDefendantRadioList(buildDefendantPartyList(claimEntity));

        return caseData;
    }

    private DynamicList buildDefendantPartyList(ClaimEntity claimEntity) {
        List<DynamicListElement> listItems = claimEntity.getClaimParties().stream()
            .filter(claimPartyEntity -> claimPartyEntity.getRole() == PartyRole.DEFENDANT)
            .map(claimPartyEntity -> DynamicListElement.builder()
                .code(claimPartyEntity.getParty().getId())
                .label("%s - %s".formatted(
                    buildPartyDisplayName(claimPartyEntity.getParty()),
                    partyService.getPartyLabel(claimEntity, claimPartyEntity.getParty().getId())
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
        PCSCase pcsCase = eventPayload.caseData();
        long caseReference = eventPayload.caseReference();

        DefendantPaperResponseRequest defendantPaperResponse = pcsCase.getDefendantPaperResponse();
        PartyEntity defendantParty = partyService
            .getPartyEntityByEntityId(pcsCase.getDefendantRadioList().getValueCode(), caseReference);

        PossessionClaimResponse possessionClaimResponse = buildPossessionClaimResponse(defendantPaperResponse, defendantParty);
        claimResponseService.saveDraftDataForParty(possessionClaimResponse, defendantParty);
        defendantResponseService.saveDefendantResponse(
            caseReference,
            possessionClaimResponse,
            defendantParty,
            JourneyType.CASEWORKER
        );

        return SubmitResponse.<State>builder()
            .confirmationBody(buildConfirmationMarkdown(eventPayload.caseReference()))
            .build();
    }

    private PossessionClaimResponse buildPossessionClaimResponse(
        DefendantPaperResponseRequest defendantPaperResponse,
        PartyEntity defendantParty
    ) {
        AddressUK address = defendantPaperResponse.getAddress();
        String firstName = defendantPaperResponse.getFirstName();
        String lastName = defendantPaperResponse.getLastName();

        DefendantContactDetails defendantContactDetails = buildDefendantContactDetails(address, firstName, lastName);
        DefendantResponses defendantResponse = buildDefendantResponses(
            address,
            firstName,
            lastName,
            defendantPaperResponse.getPhoneNumber(),
            defendantPaperResponse.getContactPreferences(),
            defendantPaperResponse.getFreeLegalAdvice(),
            defendantPaperResponse.getHasMadeCounterClaim(),
            defendantParty
        );

        return PossessionClaimResponse.builder()
            .defendantContactDetails(defendantContactDetails)
            .defendantResponses(defendantResponse)
            .build();
    }

    private DefendantContactDetails buildDefendantContactDetails(AddressUK address, String firstName, String lastName) {
        return DefendantContactDetails.builder()
            .party(
                Party.builder()
                    .address(address)
                    .firstName(firstName)
                    .lastName(lastName)
                    .build()
            ).build();
    }

    private DefendantResponses buildDefendantResponses(
        AddressUK address,
        String firstName,
        String lastName,
        String phoneNumber,
        Set<ContactPreferencesSelection> contactPreferences,
        YesNoPreferNotToSay freeLegalAdvice,
        VerticalYesNo hasMadeCounterClaim,
        PartyEntity defendantParty
    ) {
        VerticalYesNo addressConfirmed = isAddressConfirmed(address, defendantParty);
        VerticalYesNo nameConfirmed = isNameConfirmed(firstName, lastName, defendantParty);
        VerticalYesNo contactByPhone = phoneNumber != null ? VerticalYesNo.YES : VerticalYesNo.NO;

        VerticalYesNo contactByEmail;
        VerticalYesNo contactByPost;
        if (!CollectionUtils.isEmpty(contactPreferences)) {
            contactByEmail = contactPreferences.contains(ContactPreferencesSelection.BY_EMAIL) ?
                VerticalYesNo.YES : VerticalYesNo.NO;
            contactByPost = contactPreferences.contains(ContactPreferencesSelection.BY_POST) ?
                VerticalYesNo.YES : VerticalYesNo.NO;
        } else {
            contactByEmail = VerticalYesNo.NO;
            contactByPost = VerticalYesNo.NO;
        }

        return DefendantResponses.builder()
            .freeLegalAdvice(freeLegalAdvice)
            .correspondenceAddressConfirmation(addressConfirmed)
            .contactByEmail(contactByEmail)
            .contactByPost(contactByPost)
            .contactByPhone(contactByPhone)
            .makeCounterClaim(hasMadeCounterClaim)
            .defendantNameConfirmation(nameConfirmed)
            .build();
    }

    private VerticalYesNo isNameConfirmed(String firstName, String lastName, PartyEntity partyEntity) {
        if (StringUtils.isBlank(firstName) & StringUtils.isBlank(lastName)) {
            return null;
        }

        String partyFirstName = partyEntity.getFirstName();
        String partyLastName = partyEntity.getLastName();

        boolean firstNameMatch = StringUtils.isNotBlank(firstName)
            && StringUtils.isNotBlank(partyFirstName)
            && firstName.equalsIgnoreCase(partyFirstName);

        boolean lastNameMatch = StringUtils.isNotBlank(lastName)
            && StringUtils.isNotBlank(partyLastName)
            && lastName.equalsIgnoreCase(partyLastName);

        return firstNameMatch && lastNameMatch ? VerticalYesNo.YES : VerticalYesNo.NO;
    }

    private VerticalYesNo isAddressConfirmed(AddressUK newAddress, PartyEntity partyEntity) {
        if (newAddress == null) {
            return null;
        }

        AddressEntity addressEntity = partyEntity.getAddress();
        AddressUK currentAddress = addressEntity != null ? addressMapper.toAddressUK(addressEntity) : null;

        return newAddress.equals(currentAddress) ? VerticalYesNo.YES : VerticalYesNo.NO;
    }

    private String buildConfirmationMarkdown(long caseReference) {
        return """
            ---
            <div class="govuk-panel govuk-panel--confirmation govuk-!-padding-top-3 govuk-!-padding-bottom-3">
            <span class="govuk-panel__title govuk-!-font-size-36">Response submitted</span><br>
            <span class="govuk-panel__body">Case number: %s</span><br>
            </div>
            """.formatted(caseReference);
    }
}
