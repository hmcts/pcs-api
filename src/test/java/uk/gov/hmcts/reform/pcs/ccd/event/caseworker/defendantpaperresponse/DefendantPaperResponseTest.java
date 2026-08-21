package uk.gov.hmcts.reform.pcs.ccd.event.caseworker.defendantpaperresponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
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
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.ClaimResponseService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.DefendantResponseService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.model.JourneyType;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DefendantPaperResponseTest extends BaseEventTest {

    @Mock
    private PcsCaseService pcsCaseService;

    @Mock
    private PartyService partyService;

    @Mock
    private ClaimResponseService claimResponseService;

    @Mock
    private DefendantResponseService defendantResponseService;

    @Mock
    private AddressMapper addressMapper;

    @Mock
    private PcsCaseEntity pcsCaseEntity;

    @Mock
    private ClaimEntity claimEntity;

    @Mock
    private PartyEntity partyEntity;

    @Mock
    private AddressEntity addressEntity;

    @BeforeEach
    void setUp() {
        DefendantPaperResponse defendantPaperResponse = new DefendantPaperResponse(
            pcsCaseService,
            partyService,
            claimResponseService,
            defendantResponseService,
            addressMapper
        );

        setEventUnderTest(defendantPaperResponse);
    }

    @Test
    void shouldBuildDefendantPartyList() {
        // Given
        UUID claimantId = UUID.randomUUID();
        UUID defendant1Id = UUID.randomUUID();
        UUID defendant2Id = UUID.randomUUID();
        UUID underlesseeId = UUID.randomUUID();

        PartyEntity claimant = PartyEntity.builder()
            .id(claimantId)
            .firstName("John")
            .lastName("Smith")
            .nameKnown(VerticalYesNo.YES)
            .build();

        PartyEntity defendant1 = PartyEntity.builder()
            .id(defendant1Id)
            .firstName("Jane")
            .lastName("Doe")
            .nameKnown(VerticalYesNo.YES)
            .build();

        PartyEntity defendant2 = PartyEntity.builder()
            .id(defendant2Id)
            .nameKnown(VerticalYesNo.NO)
            .build();

        PartyEntity underlesseeOrMortgagee = PartyEntity.builder()
            .id(underlesseeId)
            .orgName("Bank Ltd")
            .build();

        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(pcsCaseEntity.getClaims()).thenReturn(List.of(claimEntity));
        when(claimEntity.getClaimParties()).thenReturn(List.of(
            ClaimPartyEntity.builder().party(claimant).role(PartyRole.CLAIMANT).rank(1).build(),
            ClaimPartyEntity.builder().party(defendant1).role(PartyRole.DEFENDANT).rank(1).build(),
            ClaimPartyEntity.builder().party(defendant2).role(PartyRole.DEFENDANT).rank(2).build(),
            ClaimPartyEntity.builder().party(underlesseeOrMortgagee).role(PartyRole.UNDERLESSEE_OR_MORTGAGEE)
                .rank(1).build()
        ));

        when(partyService.getPartyName(defendant1)).thenReturn("Jane Doe");
        when(partyService.getPartyLabel(claimEntity, defendant1Id)).thenReturn("Defendant 1");
        when(partyService.getPartyLabel(claimEntity, defendant2Id)).thenReturn("Defendant 2");

        // When
        PCSCase result = callStartHandler(PCSCase.builder().build());

        // Then
        List<DynamicListElement> listItems = result.getDefendantRadioList().getListItems();

        assertThat(listItems).containsExactly(
            DynamicListElement.builder().code(defendant1Id).label("Jane Doe - Defendant 1").build(),
            DynamicListElement.builder().code(defendant2Id).label("Person unknown - Defendant 2").build()
        );
    }

    @Test
    void shouldSaveDefendantResponseWithMatchingPartyData() {
        // Given
        DefendantPaperResponseRequest defendantPaperResponseRequest = DefendantPaperResponseRequest.builder()
            .freeLegalAdvice(YesNoPreferNotToSay.PREFER_NOT_TO_SAY)
            .firstName("John")
            .lastName("Smith")
            .address(AddressUK.builder().addressLine1("Address line 1").build())
            .dateOfBirth(LocalDate.of(2000, 1, 1))
            .contactPreferences(Set.of(ContactPreferencesSelection.BY_EMAIL, ContactPreferencesSelection.BY_POST))
            .emailAddress("test@email.com")
            .phoneNumber("phone number")
            .hasMadeCounterClaim(VerticalYesNo.YES)
            .build();

        UUID partyId = UUID.randomUUID();
        DynamicList dynamicList = DynamicList.builder()
            .value(DynamicListElement.builder().code(partyId).build())
            .build();

        PCSCase caseData = PCSCase.builder()
            .defendantPaperResponse(defendantPaperResponseRequest)
            .defendantRadioList(dynamicList)
            .build();

        stubPartyDetails(partyId);

        // When
        SubmitResponse<State> response = callSubmitHandler(caseData);

        // Then
        assertThat(response.getConfirmationBody())
            .contains("Response submitted")
            .contains("Case number: " + TEST_CASE_REFERENCE);

        ArgumentCaptor<PossessionClaimResponse> possessionClaimResponseCaptor =
            ArgumentCaptor.forClass(PossessionClaimResponse.class);
        verify(claimResponseService)
            .saveDraftDataForParty(possessionClaimResponseCaptor.capture(), eq(partyEntity), eq(TEST_CASE_REFERENCE));

        PossessionClaimResponse possessionClaimResponse = possessionClaimResponseCaptor.getValue();
        verify(defendantResponseService)
            .saveDefendantResponse(TEST_CASE_REFERENCE, possessionClaimResponse, partyEntity, JourneyType.CASEWORKER);

        DefendantContactDetails defendantContactDetails = possessionClaimResponse.getDefendantContactDetails();
        assertThat(defendantContactDetails).isNotNull();

        Party party = defendantContactDetails.getParty();
        assertThat(party).isNotNull();
        assertThat(party.getFirstName()).isEqualTo("John");
        assertThat(party.getLastName()).isEqualTo("Smith");
        assertThat(party.getAddress()).isEqualTo(AddressUK.builder().addressLine1("Address line 1").build());
        assertThat(party.getEmailAddress()).isEqualTo("test@email.com");
        assertThat(party.getPhoneNumber()).isEqualTo("phone number");

        DefendantResponses defendantResponses = possessionClaimResponse.getDefendantResponses();
        assertThat(defendantResponses).isNotNull();
        assertThat(defendantResponses.getFreeLegalAdvice()).isEqualTo(YesNoPreferNotToSay.PREFER_NOT_TO_SAY);
        assertThat(defendantResponses.getCorrespondenceAddressConfirmation()).isEqualTo(VerticalYesNo.YES);
        assertThat(defendantResponses.getContactByEmail()).isEqualTo(VerticalYesNo.YES);
        assertThat(defendantResponses.getContactByPost()).isEqualTo(VerticalYesNo.YES);
        assertThat(defendantResponses.getContactByPhone()).isEqualTo(VerticalYesNo.YES);
        assertThat(defendantResponses.getMakeCounterClaim()).isEqualTo(VerticalYesNo.YES);
        assertThat(defendantResponses.getDefendantNameConfirmation()).isEqualTo(VerticalYesNo.YES);
    }

    @Test
    void shouldSaveDefendantResponseWithNoData() {
        // Given
        DefendantPaperResponseRequest defendantPaperResponseRequest = DefendantPaperResponseRequest.builder().build();

        UUID partyId = UUID.randomUUID();
        DynamicList dynamicList = DynamicList.builder()
            .value(DynamicListElement.builder().code(partyId).build())
            .build();

        PCSCase caseData = PCSCase.builder()
            .defendantPaperResponse(defendantPaperResponseRequest)
            .defendantRadioList(dynamicList)
            .build();

        when(partyService.getPartyEntityByEntityId(partyId, TEST_CASE_REFERENCE)).thenReturn(partyEntity);

        // When
        SubmitResponse<State> response = callSubmitHandler(caseData);

        // Then
        assertThat(response.getConfirmationBody())
            .contains("Response submitted")
            .contains("Case number: " + TEST_CASE_REFERENCE);

        ArgumentCaptor<PossessionClaimResponse> possessionClaimResponseCaptor =
            ArgumentCaptor.forClass(PossessionClaimResponse.class);
        verify(claimResponseService)
            .saveDraftDataForParty(possessionClaimResponseCaptor.capture(), eq(partyEntity), eq(TEST_CASE_REFERENCE));

        PossessionClaimResponse possessionClaimResponse = possessionClaimResponseCaptor.getValue();
        verify(defendantResponseService)
            .saveDefendantResponse(TEST_CASE_REFERENCE, possessionClaimResponse, partyEntity, JourneyType.CASEWORKER);

        DefendantContactDetails defendantContactDetails = possessionClaimResponse.getDefendantContactDetails();
        assertThat(defendantContactDetails).isNotNull();

        Party party = defendantContactDetails.getParty();
        assertThat(party).isNotNull();
        assertThat(party.getFirstName()).isNull();
        assertThat(party.getLastName()).isNull();
        assertThat(party.getAddress()).isNull();
        assertThat(party.getEmailAddress()).isNull();
        assertThat(party.getPhoneNumber()).isNull();

        DefendantResponses defendantResponses = possessionClaimResponse.getDefendantResponses();
        assertThat(defendantResponses).isNotNull();
        assertThat(defendantResponses.getFreeLegalAdvice()).isNull();
        assertThat(defendantResponses.getCorrespondenceAddressConfirmation()).isNull();
        assertThat(defendantResponses.getContactByEmail()).isEqualTo(VerticalYesNo.NO);
        assertThat(defendantResponses.getContactByPost()).isEqualTo(VerticalYesNo.NO);
        assertThat(defendantResponses.getContactByPhone()).isEqualTo(VerticalYesNo.NO);
        assertThat(defendantResponses.getMakeCounterClaim()).isNull();
        assertThat(defendantResponses.getDefendantNameConfirmation()).isNull();
    }

    @Test
    void shouldSaveDefendantResponseWithDifferentFirstName() {
        // Given
        DefendantPaperResponseRequest defendantPaperResponseRequest = DefendantPaperResponseRequest.builder()
            .freeLegalAdvice(YesNoPreferNotToSay.PREFER_NOT_TO_SAY)
            .firstName("Jack")
            .lastName("Smith")
            .address(AddressUK.builder().addressLine1("Address line 1").build())
            .dateOfBirth(LocalDate.of(2000, 1, 1))
            .contactPreferences(Set.of(ContactPreferencesSelection.BY_EMAIL, ContactPreferencesSelection.BY_POST))
            .emailAddress("test@email.com")
            .phoneNumber("phone number")
            .hasMadeCounterClaim(VerticalYesNo.YES)
            .build();

        UUID partyId = UUID.randomUUID();
        DynamicList dynamicList = DynamicList.builder()
            .value(DynamicListElement.builder().code(partyId).build())
            .build();

        PCSCase caseData = PCSCase.builder()
            .defendantPaperResponse(defendantPaperResponseRequest)
            .defendantRadioList(dynamicList)
            .build();

        stubPartyDetails(partyId);

        // When
        SubmitResponse<State> response = callSubmitHandler(caseData);

        // Then
        assertThat(response.getConfirmationBody())
            .contains("Response submitted")
            .contains("Case number: " + TEST_CASE_REFERENCE);

        ArgumentCaptor<PossessionClaimResponse> possessionClaimResponseCaptor =
            ArgumentCaptor.forClass(PossessionClaimResponse.class);
        verify(claimResponseService)
            .saveDraftDataForParty(possessionClaimResponseCaptor.capture(), eq(partyEntity), eq(TEST_CASE_REFERENCE));

        PossessionClaimResponse possessionClaimResponse = possessionClaimResponseCaptor.getValue();
        verify(defendantResponseService)
            .saveDefendantResponse(TEST_CASE_REFERENCE, possessionClaimResponse, partyEntity, JourneyType.CASEWORKER);

        DefendantContactDetails defendantContactDetails = possessionClaimResponse.getDefendantContactDetails();
        assertThat(defendantContactDetails).isNotNull();

        Party party = defendantContactDetails.getParty();
        assertThat(party).isNotNull();
        assertThat(party.getFirstName()).isEqualTo("Jack");
        assertThat(party.getLastName()).isEqualTo("Smith");
        assertThat(party.getAddress()).isEqualTo(AddressUK.builder().addressLine1("Address line 1").build());
        assertThat(party.getEmailAddress()).isEqualTo("test@email.com");
        assertThat(party.getPhoneNumber()).isEqualTo("phone number");

        DefendantResponses defendantResponses = possessionClaimResponse.getDefendantResponses();
        assertThat(defendantResponses).isNotNull();
        assertThat(defendantResponses.getFreeLegalAdvice()).isEqualTo(YesNoPreferNotToSay.PREFER_NOT_TO_SAY);
        assertThat(defendantResponses.getCorrespondenceAddressConfirmation()).isEqualTo(VerticalYesNo.YES);
        assertThat(defendantResponses.getContactByEmail()).isEqualTo(VerticalYesNo.YES);
        assertThat(defendantResponses.getContactByPost()).isEqualTo(VerticalYesNo.YES);
        assertThat(defendantResponses.getContactByPhone()).isEqualTo(VerticalYesNo.YES);
        assertThat(defendantResponses.getMakeCounterClaim()).isEqualTo(VerticalYesNo.YES);
        assertThat(defendantResponses.getDefendantNameConfirmation()).isEqualTo(VerticalYesNo.NO);
    }

    @Test
    void shouldSaveDefendantResponseWithDifferentLastName() {
        // Given
        DefendantPaperResponseRequest defendantPaperResponseRequest = DefendantPaperResponseRequest.builder()
            .freeLegalAdvice(YesNoPreferNotToSay.PREFER_NOT_TO_SAY)
            .firstName("John")
            .lastName("Brown")
            .address(AddressUK.builder().addressLine1("Address line 1").build())
            .dateOfBirth(LocalDate.of(2000, 1, 1))
            .contactPreferences(Set.of(ContactPreferencesSelection.BY_EMAIL, ContactPreferencesSelection.BY_POST))
            .emailAddress("test@email.com")
            .phoneNumber("phone number")
            .hasMadeCounterClaim(VerticalYesNo.YES)
            .build();

        UUID partyId = UUID.randomUUID();
        DynamicList dynamicList = DynamicList.builder()
            .value(DynamicListElement.builder().code(partyId).build())
            .build();

        PCSCase caseData = PCSCase.builder()
            .defendantPaperResponse(defendantPaperResponseRequest)
            .defendantRadioList(dynamicList)
            .build();

        stubPartyDetails(partyId);

        // When
        SubmitResponse<State> response = callSubmitHandler(caseData);

        // Then
        assertThat(response.getConfirmationBody())
            .contains("Response submitted")
            .contains("Case number: " + TEST_CASE_REFERENCE);

        ArgumentCaptor<PossessionClaimResponse> possessionClaimResponseCaptor =
            ArgumentCaptor.forClass(PossessionClaimResponse.class);
        verify(claimResponseService)
            .saveDraftDataForParty(possessionClaimResponseCaptor.capture(), eq(partyEntity), eq(TEST_CASE_REFERENCE));

        PossessionClaimResponse possessionClaimResponse = possessionClaimResponseCaptor.getValue();
        verify(defendantResponseService)
            .saveDefendantResponse(TEST_CASE_REFERENCE, possessionClaimResponse, partyEntity, JourneyType.CASEWORKER);

        DefendantContactDetails defendantContactDetails = possessionClaimResponse.getDefendantContactDetails();
        assertThat(defendantContactDetails).isNotNull();

        Party party = defendantContactDetails.getParty();
        assertThat(party).isNotNull();
        assertThat(party.getFirstName()).isEqualTo("John");
        assertThat(party.getLastName()).isEqualTo("Brown");
        assertThat(party.getAddress()).isEqualTo(AddressUK.builder().addressLine1("Address line 1").build());
        assertThat(party.getEmailAddress()).isEqualTo("test@email.com");
        assertThat(party.getPhoneNumber()).isEqualTo("phone number");

        DefendantResponses defendantResponses = possessionClaimResponse.getDefendantResponses();
        assertThat(defendantResponses).isNotNull();
        assertThat(defendantResponses.getFreeLegalAdvice()).isEqualTo(YesNoPreferNotToSay.PREFER_NOT_TO_SAY);
        assertThat(defendantResponses.getCorrespondenceAddressConfirmation()).isEqualTo(VerticalYesNo.YES);
        assertThat(defendantResponses.getContactByEmail()).isEqualTo(VerticalYesNo.YES);
        assertThat(defendantResponses.getContactByPost()).isEqualTo(VerticalYesNo.YES);
        assertThat(defendantResponses.getContactByPhone()).isEqualTo(VerticalYesNo.YES);
        assertThat(defendantResponses.getMakeCounterClaim()).isEqualTo(VerticalYesNo.YES);
        assertThat(defendantResponses.getDefendantNameConfirmation()).isEqualTo(VerticalYesNo.NO);
    }

    @Test
    void shouldNotSaveEmailAddressWhenNotAContactPreference() {
        // Given
        DefendantPaperResponseRequest defendantPaperResponseRequest = DefendantPaperResponseRequest.builder()
            .freeLegalAdvice(YesNoPreferNotToSay.PREFER_NOT_TO_SAY)
            .firstName("John")
            .lastName("Smith")
            .address(AddressUK.builder().addressLine1("Address line 1").build())
            .dateOfBirth(LocalDate.of(2000, 1, 1))
            .contactPreferences(Set.of(ContactPreferencesSelection.BY_POST))
            .emailAddress("test@email.com")
            .phoneNumber("phone number")
            .hasMadeCounterClaim(VerticalYesNo.YES)
            .build();

        UUID partyId = UUID.randomUUID();
        DynamicList dynamicList = DynamicList.builder()
            .value(DynamicListElement.builder().code(partyId).build())
            .build();

        PCSCase caseData = PCSCase.builder()
            .defendantPaperResponse(defendantPaperResponseRequest)
            .defendantRadioList(dynamicList)
            .build();

        stubPartyDetails(partyId);

        // When
        SubmitResponse<State> response = callSubmitHandler(caseData);

        // Then
        assertThat(response.getConfirmationBody())
            .contains("Response submitted")
            .contains("Case number: " + TEST_CASE_REFERENCE);

        ArgumentCaptor<PossessionClaimResponse> possessionClaimResponseCaptor =
            ArgumentCaptor.forClass(PossessionClaimResponse.class);
        verify(claimResponseService)
            .saveDraftDataForParty(possessionClaimResponseCaptor.capture(), eq(partyEntity), eq(TEST_CASE_REFERENCE));

        PossessionClaimResponse possessionClaimResponse = possessionClaimResponseCaptor.getValue();
        verify(defendantResponseService)
            .saveDefendantResponse(TEST_CASE_REFERENCE, possessionClaimResponse, partyEntity, JourneyType.CASEWORKER);

        DefendantContactDetails defendantContactDetails = possessionClaimResponse.getDefendantContactDetails();
        assertThat(defendantContactDetails).isNotNull();

        Party party = defendantContactDetails.getParty();
        assertThat(party).isNotNull();
        assertThat(party.getFirstName()).isEqualTo("John");
        assertThat(party.getLastName()).isEqualTo("Smith");
        assertThat(party.getAddress()).isEqualTo(AddressUK.builder().addressLine1("Address line 1").build());
        assertThat(party.getEmailAddress()).isNull();
        assertThat(party.getPhoneNumber()).isEqualTo("phone number");

        DefendantResponses defendantResponses = possessionClaimResponse.getDefendantResponses();
        assertThat(defendantResponses).isNotNull();
        assertThat(defendantResponses.getFreeLegalAdvice()).isEqualTo(YesNoPreferNotToSay.PREFER_NOT_TO_SAY);
        assertThat(defendantResponses.getCorrespondenceAddressConfirmation()).isEqualTo(VerticalYesNo.YES);
        assertThat(defendantResponses.getContactByEmail()).isEqualTo(VerticalYesNo.NO);
        assertThat(defendantResponses.getContactByPost()).isEqualTo(VerticalYesNo.YES);
        assertThat(defendantResponses.getContactByPhone()).isEqualTo(VerticalYesNo.YES);
        assertThat(defendantResponses.getMakeCounterClaim()).isEqualTo(VerticalYesNo.YES);
        assertThat(defendantResponses.getDefendantNameConfirmation()).isEqualTo(VerticalYesNo.YES);
    }

    @Test
    void shouldSaveDefendantResponseWithDifferentAddress() {
        // Given
        DefendantPaperResponseRequest defendantPaperResponseRequest = DefendantPaperResponseRequest.builder()
            .freeLegalAdvice(YesNoPreferNotToSay.PREFER_NOT_TO_SAY)
            .firstName("John")
            .lastName("Smith")
            .address(AddressUK.builder().addressLine1("New address").build())
            .dateOfBirth(LocalDate.of(2000, 1, 1))
            .contactPreferences(Set.of(ContactPreferencesSelection.BY_EMAIL, ContactPreferencesSelection.BY_POST))
            .emailAddress("test@email.com")
            .phoneNumber("phone number")
            .hasMadeCounterClaim(VerticalYesNo.YES)
            .build();

        UUID partyId = UUID.randomUUID();
        DynamicList dynamicList = DynamicList.builder()
            .value(DynamicListElement.builder().code(partyId).build())
            .build();

        PCSCase caseData = PCSCase.builder()
            .defendantPaperResponse(defendantPaperResponseRequest)
            .defendantRadioList(dynamicList)
            .build();

        stubPartyDetails(partyId);

        // When
        SubmitResponse<State> response = callSubmitHandler(caseData);

        // Then
        assertThat(response.getConfirmationBody())
            .contains("Response submitted")
            .contains("Case number: " + TEST_CASE_REFERENCE);

        ArgumentCaptor<PossessionClaimResponse> possessionClaimResponseCaptor =
            ArgumentCaptor.forClass(PossessionClaimResponse.class);
        verify(claimResponseService)
            .saveDraftDataForParty(possessionClaimResponseCaptor.capture(), eq(partyEntity), eq(TEST_CASE_REFERENCE));

        PossessionClaimResponse possessionClaimResponse = possessionClaimResponseCaptor.getValue();
        verify(defendantResponseService)
            .saveDefendantResponse(TEST_CASE_REFERENCE, possessionClaimResponse, partyEntity, JourneyType.CASEWORKER);

        DefendantContactDetails defendantContactDetails = possessionClaimResponse.getDefendantContactDetails();
        assertThat(defendantContactDetails).isNotNull();

        Party party = defendantContactDetails.getParty();
        assertThat(party).isNotNull();
        assertThat(party.getFirstName()).isEqualTo("John");
        assertThat(party.getLastName()).isEqualTo("Smith");
        assertThat(party.getAddress()).isEqualTo(AddressUK.builder().addressLine1("New address").build());
        assertThat(party.getEmailAddress()).isEqualTo("test@email.com");
        assertThat(party.getPhoneNumber()).isEqualTo("phone number");

        DefendantResponses defendantResponses = possessionClaimResponse.getDefendantResponses();
        assertThat(defendantResponses).isNotNull();
        assertThat(defendantResponses.getFreeLegalAdvice()).isEqualTo(YesNoPreferNotToSay.PREFER_NOT_TO_SAY);
        assertThat(defendantResponses.getCorrespondenceAddressConfirmation()).isEqualTo(VerticalYesNo.NO);
        assertThat(defendantResponses.getContactByEmail()).isEqualTo(VerticalYesNo.YES);
        assertThat(defendantResponses.getContactByPost()).isEqualTo(VerticalYesNo.YES);
        assertThat(defendantResponses.getContactByPhone()).isEqualTo(VerticalYesNo.YES);
        assertThat(defendantResponses.getMakeCounterClaim()).isEqualTo(VerticalYesNo.YES);
        assertThat(defendantResponses.getDefendantNameConfirmation()).isEqualTo(VerticalYesNo.YES);
    }

    private void stubPartyDetails(UUID partyId) {
        when(partyService.getPartyEntityByEntityId(partyId, TEST_CASE_REFERENCE)).thenReturn(partyEntity);
        when(partyEntity.getFirstName()).thenReturn("John");
        when(partyEntity.getLastName()).thenReturn("Smith");
        when(partyEntity.getAddress()).thenReturn(addressEntity);
        when(addressMapper.toAddressUK(addressEntity))
            .thenReturn(AddressUK.builder().addressLine1("Address line 1").build());
    }
}
