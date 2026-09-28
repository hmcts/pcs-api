package uk.gov.hmcts.reform.pcs.ccd.service.caseworker.manageparty;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.AddPartyDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.PartyType;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;
import uk.gov.hmcts.reform.pcs.ccd.task.SendClaimPackTaskData;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.exception.PartyNotFoundException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddPartyServiceTest {

    @Mock
    private PartyRepository partyRepository;
    @Mock
    private ClaimRepository claimRepository;
    @Mock
    private AddressMapper addressMapper;
    @Mock
    private SchedulerClient schedulerClient;
    @Mock(strictness = LENIENT)
    private Clock utcClock;
    @Mock
    private PcsCaseEntity pcsCaseEntity;
    @Mock
    private ClaimEntity claimEntity;
    @Captor
    private ArgumentCaptor<PartyEntity> partyEntityCaptor;
    @Captor
    private ArgumentCaptor<SchedulableInstance<SendClaimPackTaskData>> schedulableInstanceCaptor;

    private AddPartyService underTest;

    private static final LocalDateTime TEST_UTC_DATE_TIME = LocalDate.of(2025, 8, 27)
        .atTime(12, 51, 19);

    @BeforeEach
    void setUp() {
        underTest = new AddPartyService(partyRepository, claimRepository, addressMapper, schedulerClient, utcClock);
    }

    @Test
    void shouldPersistClaimantPartyAndLinkToClaim() {
        // Given
        AddPartyDetails addPartyDetails = AddPartyDetails.builder()
            .addPartyType(PartyType.CLAIMANT)
            .claimantFirstName("Jane")
            .claimantLastName("Doe")
            .build();

        // When
        underTest.addParty(addPartyDetails, pcsCaseEntity, claimEntity, null);

        // Then
        verify(pcsCaseEntity).addParty(partyEntityCaptor.capture());
        PartyEntity createdParty = partyEntityCaptor.getValue();
        assertThat(createdParty.getFirstName()).isEqualTo("Jane");
        assertThat(createdParty.getLastName()).isEqualTo("Doe");
        assertThat(createdParty.getNameKnown()).isEqualTo(VerticalYesNo.YES);

        verify(partyRepository).save(createdParty);
        verify(claimEntity).addParty(createdParty, PartyRole.CLAIMANT, null);
        verify(claimRepository).save(claimEntity);
        verify(partyRepository, never()).findById(any());
        verifyNoInteractions(schedulerClient);
    }

    @Test
    void shouldPersistDefendantPartyAndLinkToClaim() {
        // Given
        AddPartyDetails addPartyDetails = AddPartyDetails.builder()
            .addPartyType(PartyType.DEFENDANT)
            .firstName("John")
            .lastName("Smith")
            .build();

        UUID caseId = UUID.randomUUID();
        when(pcsCaseEntity.getId()).thenReturn(caseId);
        when(utcClock.instant()).thenReturn(TEST_UTC_DATE_TIME.toInstant(ZoneOffset.UTC));
        when(utcClock.getZone()).thenReturn(ZoneOffset.UTC);

        // When
        underTest.addParty(addPartyDetails, pcsCaseEntity, claimEntity, null);

        // Then
        verify(pcsCaseEntity).addParty(partyEntityCaptor.capture());
        PartyEntity createdParty = partyEntityCaptor.getValue();
        assertThat(createdParty.getFirstName()).isEqualTo("John");
        assertThat(createdParty.getLastName()).isEqualTo("Smith");
        assertThat(createdParty.getNameKnown()).isEqualTo(VerticalYesNo.YES);

        verify(partyRepository).save(createdParty);
        verify(claimEntity).addParty(createdParty, PartyRole.DEFENDANT, null);
        verify(claimRepository).save(claimEntity);
        verify(partyRepository, never()).findById(any());

        verify(schedulerClient).scheduleIfNotExists(schedulableInstanceCaptor.capture());
        SchedulableInstance<SendClaimPackTaskData> schedulableInstance = schedulableInstanceCaptor.getValue();
        SendClaimPackTaskData taskData = schedulableInstance.getTaskInstance().getData();
        assertThat(taskData.getCaseId()).isEqualTo(caseId);
        assertThat(schedulableInstance.getNextExecutionTime(Instant.now()))
            .isEqualTo(Instant.parse("2025-08-27T12:51:19Z"));
    }

    @Test
    void shouldPersistLitigationFriendAndLinkToClaim() {
        // Given
        UUID actingForPartyId = UUID.randomUUID();
        PartyEntity actingForParty = mock(PartyEntity.class);
        when(partyRepository.findById(actingForPartyId)).thenReturn(Optional.of(actingForParty));

        AddPartyDetails addPartyDetails = AddPartyDetails.builder()
            .addPartyType(PartyType.LITIGATION_FRIEND)
            .litigationFriendOrganisationName("Acme Ltd")
            .litigationFriendFirstName("Bob")
            .litigationFriendLastName("Jones")
            .build();

        // When
        underTest.addParty(addPartyDetails, pcsCaseEntity, claimEntity, actingForPartyId);

        // Then
        verify(pcsCaseEntity).addParty(partyEntityCaptor.capture());
        PartyEntity createdParty = partyEntityCaptor.getValue();
        assertThat(createdParty.getOrgName()).isEqualTo("Acme Ltd");
        assertThat(createdParty.getNameKnown()).isEqualTo(VerticalYesNo.YES);

        verify(partyRepository).save(createdParty);
        verify(claimEntity).addParty(createdParty, PartyRole.LITIGATION_FRIEND, actingForParty);
        verify(claimRepository).save(claimEntity);
        verify(partyRepository).findById(actingForPartyId);
        verifyNoInteractions(schedulerClient);
    }

    @Test
    void shouldThrowWhenActingForPartyNotFound() {
        // Given
        UUID actingForPartyId = UUID.randomUUID();
        when(partyRepository.findById(actingForPartyId)).thenReturn(Optional.empty());

        AddPartyDetails addPartyDetails = AddPartyDetails.builder()
            .addPartyType(PartyType.LITIGATION_FRIEND)
            .litigationFriendFirstName("Bob")
            .litigationFriendLastName("Jones")
            .build();

        // When
        assertThatThrownBy(() -> underTest.addParty(addPartyDetails, pcsCaseEntity, claimEntity, actingForPartyId))
            .isInstanceOf(PartyNotFoundException.class);
        // Then
        verify(claimRepository, never()).save(any());
        verifyNoInteractions(schedulerClient);
    }

    @Test
    void shouldThrowWhenActingForPartyIdNotProvidedForLitigationFriend() {
        // Given
        AddPartyDetails addPartyDetails = AddPartyDetails.builder()
            .addPartyType(PartyType.LITIGATION_FRIEND)
            .litigationFriendFirstName("Bob")
            .litigationFriendLastName("Jones")
            .build();

        // When
        assertThatThrownBy(() -> underTest.addParty(addPartyDetails, pcsCaseEntity, claimEntity, null))
            .isInstanceOf(NullPointerException.class);

        // Then
        verify(partyRepository, never()).findById(any());
        verify(claimRepository, never()).save(any());
        verifyNoInteractions(schedulerClient);
    }

    @Test
    void shouldSetContactDetailsWhenProvided() {
        // Given
        AddressUK address = mock(AddressUK.class);
        AddressEntity mappedAddress = mock(AddressEntity.class);
        when(addressMapper.toAddressEntityAndNormalise(address)).thenReturn(mappedAddress);

        AddPartyDetails addPartyDetails = AddPartyDetails.builder()
            .addPartyType(PartyType.CLAIMANT)
            .claimantFirstName("Jane")
            .claimantLastName("Doe")
            .claimantAddress(address)
            .claimantEmail("jane@test.com")
            .claimantPhoneNumber("07000000000")
            .build();

        // When
        underTest.addParty(addPartyDetails, pcsCaseEntity, claimEntity, null);

        // Then
        verify(pcsCaseEntity).addParty(partyEntityCaptor.capture());
        PartyEntity partyEntity = partyEntityCaptor.getValue();
        assertThat(partyEntity.getAddress()).isEqualTo(mappedAddress);
        assertThat(partyEntity.getAddressKnown()).isEqualTo(VerticalYesNo.YES);
        assertThat(partyEntity.getEmailAddress()).isEqualTo("jane@test.com");
        assertThat(partyEntity.getPhoneNumber()).isEqualTo("07000000000");
        assertThat(partyEntity.getPhoneNumberProvided()).isEqualTo(VerticalYesNo.YES);
    }

    @Test
    void shouldNotSetAddressOrPhoneWhenNotProvided() {
        // Given
        AddPartyDetails addPartyDetails = AddPartyDetails.builder()
            .addPartyType(PartyType.CLAIMANT)
            .claimantFirstName("Jane")
            .claimantLastName("Doe")
            .build();

        // When
        underTest.addParty(addPartyDetails, pcsCaseEntity, claimEntity, null);

        // Then
        verify(pcsCaseEntity).addParty(partyEntityCaptor.capture());
        PartyEntity partyEntity = partyEntityCaptor.getValue();
        assertThat(partyEntity.getAddress()).isNull();
        assertThat(partyEntity.getAddressKnown()).isEqualTo(VerticalYesNo.NO);
        assertThat(partyEntity.getPhoneNumber()).isNull();
        assertThat(partyEntity.getPhoneNumberProvided()).isEqualTo(VerticalYesNo.NO);
        verifyNoInteractions(addressMapper);
    }
}
