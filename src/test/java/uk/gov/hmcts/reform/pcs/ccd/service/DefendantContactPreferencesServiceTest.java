package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ContactPreferencesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class DefendantContactPreferencesServiceTest {

    @Mock
    private PartyRepository partyRepository;

    @Mock
    private SecurityContextService securityContextService;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private DefendantContactPreferencesService service;

    @Captor
    private ArgumentCaptor<PartyEntity> partyCaptor;

    private static final UUID TEST_IDAM_ID = UUID.randomUUID();
    private static final UUID TEST_PARTY_ID = UUID.randomUUID();
    private PartyEntity testParty;

    @BeforeEach
    void setUp() {
        testParty = new PartyEntity();
        testParty.setId(TEST_PARTY_ID);
        testParty.setIdamId(TEST_IDAM_ID);
    }

    @Test
    void shouldSaveDraftDataWithAllFieldsProvided() {
        // Given
        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .contactByEmail(YesOrNo.YES)
            .contactByPhone(YesOrNo.NO)
            .contactByText(YesOrNo.YES)
            .contactByPost(YesOrNo.NO)
            .phoneNumber("07123456789")
            .email("defendant@example.com")
            .address("123 Test Street")
            .build();

        AddressEntity addressEntity = new AddressEntity();
        when(securityContextService.getCurrentUserId()).thenReturn(TEST_IDAM_ID);
        when(partyRepository.findByIdamId(TEST_IDAM_ID)).thenReturn(Optional.of(testParty));
        when(modelMapper.map(response.getAddress(), AddressEntity.class)).thenReturn(addressEntity);

        // When
        service.saveDraftData(response);

        // Then
        verify(partyRepository, times(2)).save(partyCaptor.capture());
        PartyEntity savedParty = partyCaptor.getValue();
        assertThat(savedParty.getPhoneNumber()).isEqualTo("07123456789");
        assertThat(savedParty.getEmailAddress()).isEqualTo("defendant@example.com");
        assertThat(savedParty.getAddress()).isEqualTo(addressEntity);

        ContactPreferencesEntity savedPrefs = savedParty.getContactPreferences();
        assertThat(savedPrefs.getContactByEmail()).isTrue();
        assertThat(savedPrefs.getContactByPhone()).isFalse();
        assertThat(savedPrefs.getContactByText()).isTrue();
        assertThat(savedPrefs.getContactByPost()).isFalse();
    }

    @Test
    void shouldSaveOnlyPhoneNumber() {
        // Given
        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .contactByPhone(YesOrNo.YES)
            .phoneNumber("07123456789")
            .build();

        when(securityContextService.getCurrentUserId()).thenReturn(TEST_IDAM_ID);
        when(partyRepository.findByIdamId(TEST_IDAM_ID)).thenReturn(Optional.of(testParty));

        // When
        service.saveDraftData(response);

        // Then
        verify(partyRepository, times(2)).save(partyCaptor.capture());
        PartyEntity savedParty = partyCaptor.getValue();
        assertThat(savedParty.getPhoneNumber()).isEqualTo("07123456789");
        assertThat(savedParty.getEmailAddress()).isNull();
    }

    @Test
    void shouldSaveOnlyEmail() {
        // Given
        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .contactByEmail(YesOrNo.YES)
            .email("defendant@example.com")
            .build();

        when(securityContextService.getCurrentUserId()).thenReturn(TEST_IDAM_ID);
        when(partyRepository.findByIdamId(TEST_IDAM_ID)).thenReturn(Optional.of(testParty));

        // When
        service.saveDraftData(response);

        // Then
        verify(partyRepository, times(2)).save(partyCaptor.capture());
        PartyEntity savedParty = partyCaptor.getValue();
        assertThat(savedParty.getEmailAddress()).isEqualTo("defendant@example.com");
        assertThat(savedParty.getPhoneNumber()).isNull();
    }

    @Test
    void shouldDefaultNullPreferencesToFalse() {
        // Given
        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .contactByEmail(null)
            .contactByPhone(null)
            .contactByText(YesOrNo.YES)
            .contactByPost(null)
            .build();

        when(securityContextService.getCurrentUserId()).thenReturn(TEST_IDAM_ID);
        when(partyRepository.findByIdamId(TEST_IDAM_ID)).thenReturn(Optional.of(testParty));

        // When
        service.saveDraftData(response);

        // Then
        verify(partyRepository).save(partyCaptor.capture());
        ContactPreferencesEntity savedPrefs = partyCaptor.getValue().getContactPreferences();
        assertThat(savedPrefs.getContactByEmail()).isFalse();
        assertThat(savedPrefs.getContactByPhone()).isFalse();
        assertThat(savedPrefs.getContactByText()).isTrue();
        assertThat(savedPrefs.getContactByPost()).isFalse();
    }

    @Test
    void shouldNotUpdatePartyWhenContactDetailsAreBlank() {
        // Given
        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .contactByEmail(YesOrNo.YES)
            .phoneNumber("")
            .email("   ")
            .address(null)
            .build();

        when(securityContextService.getCurrentUserId()).thenReturn(TEST_IDAM_ID);
        when(partyRepository.findByIdamId(TEST_IDAM_ID)).thenReturn(Optional.of(testParty));

        // When
        service.saveDraftData(response);

        // Then
        verify(partyRepository).save(partyCaptor.capture());
        PartyEntity savedParty = partyCaptor.getValue();
        assertThat(savedParty.getContactPreferences()).isNotNull();
        assertThat(savedParty.getContactPreferences().getContactByEmail()).isTrue();
    }

    @Test
    void shouldThrowExceptionWhenCurrentUserIdamIdIsNull() {
        // Given
        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .contactByEmail(YesOrNo.YES)
            .build();

        when(securityContextService.getCurrentUserId()).thenReturn(null);

        // When
        assertThatThrownBy(() -> service.saveDraftData(response))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Current user IDAM ID is null");

        // Then
        verifyNoInteractions(partyRepository);
    }

    @Test
    void shouldThrowExceptionWhenPartyNotFound() {
        // Given
        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .contactByEmail(YesOrNo.YES)
            .build();

        when(securityContextService.getCurrentUserId()).thenReturn(TEST_IDAM_ID);
        when(partyRepository.findByIdamId(TEST_IDAM_ID)).thenReturn(Optional.empty());

        // When
        assertThatThrownBy(() -> service.saveDraftData(response))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No party found for IDAM ID:");

        // Then
        verify(partyRepository).findByIdamId(TEST_IDAM_ID);
    }

    @Test
    void shouldHandleAllPreferencesSetToNo() {
        // Given
        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .contactByEmail(YesOrNo.NO)
            .contactByPhone(YesOrNo.NO)
            .contactByText(YesOrNo.NO)
            .contactByPost(YesOrNo.NO)
            .build();

        when(securityContextService.getCurrentUserId()).thenReturn(TEST_IDAM_ID);
        when(partyRepository.findByIdamId(TEST_IDAM_ID)).thenReturn(Optional.of(testParty));

        // When
        service.saveDraftData(response);

        // Then
        verify(partyRepository).save(partyCaptor.capture());
        ContactPreferencesEntity savedPrefs = partyCaptor.getValue().getContactPreferences();
        assertThat(savedPrefs.getContactByEmail()).isFalse();
        assertThat(savedPrefs.getContactByPhone()).isFalse();
        assertThat(savedPrefs.getContactByText()).isFalse();
        assertThat(savedPrefs.getContactByPost()).isFalse();
    }

    @Test
    void shouldHandleAllPreferencesSetToYes() {

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .contactByEmail(YesOrNo.YES)
            .contactByPhone(YesOrNo.YES)
            .contactByText(YesOrNo.YES)
            .contactByPost(YesOrNo.YES)
            .phoneNumber("07123456789")
            .email("test@example.com")
            .address("123 Test Street, London, SW1A 1AA")
            .build();

        AddressEntity addressEntity = AddressEntity.builder()
            .addressLine1("123 Test Street")
            .postTown("London")
            .postcode("SW1A 1AA")
            .build();

        when(securityContextService.getCurrentUserId()).thenReturn(TEST_IDAM_ID);
        when(partyRepository.findByIdamId(TEST_IDAM_ID)).thenReturn(Optional.of(testParty));
        when(modelMapper.map(response.getAddress(), AddressEntity.class)).thenReturn(addressEntity);

        // When
        service.saveDraftData(response);

        // Then
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
        assertThat(savedPrefs.getContactByEmail()).isTrue();
        assertThat(savedPrefs.getContactByPhone()).isTrue();
        assertThat(savedPrefs.getContactByText()).isTrue();
        assertThat(savedPrefs.getContactByPost()).isTrue();
    }
}
