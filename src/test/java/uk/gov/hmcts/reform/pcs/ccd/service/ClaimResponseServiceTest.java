package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantContactDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ContactPreferencesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.ClaimResponseService;
import uk.gov.hmcts.reform.pcs.exception.PartyNotFoundException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimResponseServiceTest {

    private static final UUID TEST_IDAM_ID = UUID.randomUUID();
    private static final UUID TEST_PARTY_ID = UUID.randomUUID();
    private static final long TEST_CASE_REFERENCE = 1234567890L;
    private static final AddressUK TEST_ADDRESS = AddressUK.builder()
        .addressLine1("123 Test Street")
        .postTown("London")
        .postCode("SW1A 1AA")
        .build();

    @Mock
    private PartyService partyService;
    @Mock
    private PartyRepository partyRepository;
    @Mock
    private SecurityContextService securityContextService;
    @Mock
    private ModelMapper modelMapper;

    private ClaimResponseService underTest;

    @Captor
    private ArgumentCaptor<PartyEntity> partyCaptor;

    private PartyEntity testParty;

    @BeforeEach
    void setUp() {
        testParty = new PartyEntity();
        testParty.setId(TEST_PARTY_ID);
        testParty.setIdamId(TEST_IDAM_ID);

        underTest = new ClaimResponseService(partyService, partyRepository, securityContextService, modelMapper);
    }

    @Test
    void shouldSaveDraftDataWithAllFieldsProvided() {
        // Given
        final PossessionClaimResponse response = buildResponse(
            Party.builder()
                .phoneNumber("07123456789")
                .emailAddress("defendant@example.com")
                .address(TEST_ADDRESS)
                .build(),
            DefendantResponses.builder()
                .contactByEmail(YesOrNo.YES)
                .contactByPost(YesOrNo.NO)
                .contactByPhone(VerticalYesNo.YES)
                .contactByText(VerticalYesNo.YES)
                .build()
        );

        final AddressEntity addressEntity = new AddressEntity();
        when(securityContextService.getCurrentUserId()).thenReturn(TEST_IDAM_ID);
        when(partyService.getPartyEntityByIdamId(TEST_IDAM_ID, TEST_CASE_REFERENCE)).thenReturn(testParty);
        when(modelMapper.map(TEST_ADDRESS, AddressEntity.class)).thenReturn(addressEntity);

        // When
        underTest.saveDraftData(response, TEST_CASE_REFERENCE);

        // Then
        verify(partyRepository, times(2)).save(partyCaptor.capture());
        PartyEntity savedParty = partyCaptor.getValue();
        assertThat(savedParty.getPhoneNumber()).isEqualTo("07123456789");
        assertThat(savedParty.getEmailAddress()).isEqualTo("defendant@example.com");
        assertThat(savedParty.getAddress()).isEqualTo(addressEntity);

        ContactPreferencesEntity savedPrefs = savedParty.getContactPreferences();
        assertThat(savedPrefs.getContactByPhone()).isEqualTo(VerticalYesNo.YES);
        assertThat(savedPrefs.getContactByText()).isEqualTo(VerticalYesNo.YES);
        assertThat(savedPrefs.getContactByEmail()).isEqualTo(YesOrNo.YES);
        assertThat(savedPrefs.getContactByPost()).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldSaveOnlyPhoneNumber() {
        // Given
        final PossessionClaimResponse response = buildResponse(
            Party.builder()
                .phoneNumber("07123456789")
                .address(TEST_ADDRESS)
                .build(),
            DefendantResponses.builder()
                .contactByPhone(VerticalYesNo.YES)
                .build()
        );

        when(securityContextService.getCurrentUserId()).thenReturn(TEST_IDAM_ID);
        when(partyService.getPartyEntityByIdamId(TEST_IDAM_ID, TEST_CASE_REFERENCE)).thenReturn(testParty);

        // When
        underTest.saveDraftData(response, TEST_CASE_REFERENCE);

        // Then
        verify(partyRepository, times(2)).save(partyCaptor.capture());
        PartyEntity savedParty = partyCaptor.getValue();
        assertThat(savedParty.getPhoneNumber()).isEqualTo("07123456789");
        assertThat(savedParty.getEmailAddress()).isNull();
    }

    @Test
    void shouldSaveOnlyEmail() {
        // Given
        final PossessionClaimResponse response = buildResponse(
            Party.builder()
                .emailAddress("defendant@example.com")
                .build(),
            DefendantResponses.builder()
                .contactByEmail(YesOrNo.YES)
                .build()
        );

        when(securityContextService.getCurrentUserId()).thenReturn(TEST_IDAM_ID);
        when(partyService.getPartyEntityByIdamId(TEST_IDAM_ID, TEST_CASE_REFERENCE)).thenReturn(testParty);

        // When
        underTest.saveDraftData(response, TEST_CASE_REFERENCE);

        // Then
        verify(partyRepository).save(partyCaptor.capture());
        PartyEntity savedParty = partyCaptor.getValue();
        assertThat(savedParty.getEmailAddress()).isEqualTo("defendant@example.com");
        assertThat(savedParty.getPhoneNumber()).isNull();
    }

    @Test
    void shouldNotUpdatePartyWhenContactDetailsAreBlank() {
        // Given
        final PossessionClaimResponse response = buildResponse(
            Party.builder()
                .phoneNumber("")
                .emailAddress("   ")
                .build(),
            DefendantResponses.builder()
                .contactByEmail(YesOrNo.YES)
                .build()
        );

        when(securityContextService.getCurrentUserId()).thenReturn(TEST_IDAM_ID);
        when(partyService.getPartyEntityByIdamId(TEST_IDAM_ID, TEST_CASE_REFERENCE)).thenReturn(testParty);

        // When
        underTest.saveDraftData(response, TEST_CASE_REFERENCE);

        // Then
        verify(partyRepository).save(partyCaptor.capture());
        PartyEntity savedParty = partyCaptor.getValue();
        assertThat(savedParty.getContactPreferences()).isNotNull();
        assertThat(savedParty.getContactPreferences().getContactByEmail()).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldThrowExceptionWhenCurrentUserIdamIdIsNull() {
        // Given
        PossessionClaimResponse response = buildResponse(
            Party.builder().build(),
            DefendantResponses.builder().build()
        );

        when(securityContextService.getCurrentUserId()).thenReturn(null);

        // When
        assertThatThrownBy(() -> underTest.saveDraftData(response, TEST_CASE_REFERENCE))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Current user IDAM ID is null");

        // Then
        verifyNoInteractions(partyRepository);
    }

    @Test
    void shouldPropagateExceptionWhenPartyNotFound() {
        // Given
        final PossessionClaimResponse response = buildResponse(
            Party.builder().build(),
            DefendantResponses.builder().build()
        );

        when(securityContextService.getCurrentUserId()).thenReturn(TEST_IDAM_ID);

        PartyNotFoundException expectedException = new PartyNotFoundException("test exception");
        when(partyService.getPartyEntityByIdamId(TEST_IDAM_ID, TEST_CASE_REFERENCE))
            .thenThrow(expectedException);

        // When / Then
        assertThatThrownBy(() -> underTest.saveDraftData(response, TEST_CASE_REFERENCE))
            .isSameAs(expectedException);

    }

    @Test
    void shouldHandleAllPreferencesSetToNo() {
        // Given
        final PossessionClaimResponse response = buildResponse(Party.builder()
                .build(),
            DefendantResponses.builder()
                .contactByEmail(YesOrNo.YES)
                .contactByPhone(VerticalYesNo.NO)
                //no text option is possible when contact by phone = no
                .contactByPost(YesOrNo.YES)
                .build()
        );

        when(securityContextService.getCurrentUserId()).thenReturn(TEST_IDAM_ID);
        when(partyService.getPartyEntityByIdamId(TEST_IDAM_ID, TEST_CASE_REFERENCE)).thenReturn(testParty);

        // When
        underTest.saveDraftData(response, TEST_CASE_REFERENCE);

        // Then
        verify(partyRepository).save(partyCaptor.capture());
        ContactPreferencesEntity savedPrefs = partyCaptor.getValue().getContactPreferences();
        assertThat(savedPrefs.getContactByPhone()).isEqualTo(VerticalYesNo.NO);
        assertThat(savedPrefs.getContactByText()).isEqualTo(null);
        assertThat(savedPrefs.getContactByEmail()).isEqualTo(YesOrNo.YES);
        assertThat(savedPrefs.getContactByPost()).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldHandleAllPreferencesSetToYes() {
        // Given
        final PossessionClaimResponse response = buildResponse(
            Party.builder()
                .phoneNumber("07123456789")
                .emailAddress("test@example.com")
                .address(TEST_ADDRESS)
                .build(),
            DefendantResponses.builder()
                .contactByEmail(YesOrNo.YES)
                .contactByPhone(VerticalYesNo.YES)
                .contactByText(VerticalYesNo.YES)
                .contactByPost(YesOrNo.YES)
                .build()
        );

        final AddressEntity addressEntity = AddressEntity.builder()
            .addressLine1("123 Test Street")
            .postTown("London")
            .postcode("SW1A 1AA")
            .build();

        when(securityContextService.getCurrentUserId()).thenReturn(TEST_IDAM_ID);
        when(partyService.getPartyEntityByIdamId(TEST_IDAM_ID, TEST_CASE_REFERENCE)).thenReturn(testParty);
        when(modelMapper.map(TEST_ADDRESS, AddressEntity.class)).thenReturn(addressEntity);

        // When
        underTest.saveDraftData(response, TEST_CASE_REFERENCE);

        // Then
        //Once for address being created, once for preferences being
        verify(partyRepository, times(2)).save(partyCaptor.capture());
        PartyEntity savedParty = partyCaptor.getValue();

        assertThat(savedParty.getAddress()).isNotNull();
        assertThat(savedParty.getAddress()).isEqualTo(addressEntity);
        assertThat(savedParty.getAddress().getAddressLine1()).isEqualTo("123 Test Street");
        assertThat(savedParty.getAddress().getPostTown()).isEqualTo("London");
        assertThat(savedParty.getAddress().getPostcode()).isEqualTo("SW1A 1AA");

        assertThat(savedParty.getPhoneNumber()).isEqualTo("07123456789");
        assertThat(savedParty.getEmailAddress()).isEqualTo("test@example.com");

        ContactPreferencesEntity savedPrefs = savedParty.getContactPreferences();
        assertThat(savedPrefs.getContactByPhone()).isEqualTo(VerticalYesNo.YES);
        assertThat(savedPrefs.getContactByText()).isEqualTo(VerticalYesNo.YES);
        assertThat(savedPrefs.getContactByEmail()).isEqualTo(YesOrNo.YES);
        assertThat(savedPrefs.getContactByPost()).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldUpdateFirstNameAndLastName() {
        // Given
        final PossessionClaimResponse response = buildResponse(
            Party.builder()
                .firstName("John")
                .lastName("Doe")
                .build(),
            DefendantResponses.builder()
                .contactByEmail(YesOrNo.YES)
                .build()
        );

        when(securityContextService.getCurrentUserId()).thenReturn(TEST_IDAM_ID);
        when(partyService.getPartyEntityByIdamId(TEST_IDAM_ID, TEST_CASE_REFERENCE)).thenReturn(testParty);

        // When
        underTest.saveDraftData(response, TEST_CASE_REFERENCE);

        // Then
        verify(partyRepository).save(partyCaptor.capture());
        PartyEntity savedParty = partyCaptor.getValue();
        assertThat(savedParty.getFirstName()).isEqualTo("John");
        assertThat(savedParty.getLastName()).isEqualTo("Doe");
    }

    @Test
    void shouldNotUpdateFirstNameAndLastNameWhenBlank() {
        // Given
        final PossessionClaimResponse response = buildResponse(
            Party.builder()
                .firstName("")
                .lastName("   ")
                .build(),
            DefendantResponses.builder()
                .contactByEmail(YesOrNo.YES)
                .build()
        );

        testParty.setFirstName("ExistingFirst");
        testParty.setLastName("ExistingLast");

        when(securityContextService.getCurrentUserId()).thenReturn(TEST_IDAM_ID);
        when(partyService.getPartyEntityByIdamId(TEST_IDAM_ID, TEST_CASE_REFERENCE)).thenReturn(testParty);

        // When
        underTest.saveDraftData(response, TEST_CASE_REFERENCE);

        // Then
        verify(partyRepository).save(partyCaptor.capture());
        PartyEntity savedParty = partyCaptor.getValue();
        assertThat(savedParty.getFirstName()).isEqualTo("ExistingFirst");
        assertThat(savedParty.getLastName()).isEqualTo("ExistingLast");
    }

    private PossessionClaimResponse buildResponse(Party party, DefendantResponses defendantResponses) {
        return PossessionClaimResponse.builder()
            .defendantContactDetails(DefendantContactDetails.builder().party(party).build())
            .defendantResponses(defendantResponses)
            .build();
    }
}
