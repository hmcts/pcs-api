package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaim;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CounterClaimServiceTest {

    private static final long CASE_REFERENCE = 1234567890L;
    private static final UUID CLAIM_ID = UUID.randomUUID();
    private static final UUID COUNTER_CLAIM_ID = UUID.randomUUID();
    private static final Clock FIXED_UTC_CLOCK = Clock.fixed(
        Instant.parse("2026-04-22T21:00:00Z"), ZoneOffset.UTC);

    @Mock
    private PartyRepository partyRepository;
    @Mock
    private ClaimRepository claimRepository;
    @Mock
    private CounterClaimRepository counterClaimRepository;
    @Mock
    private CounterClaimFeeCalculator counterClaimFeeCalculator;

    @Mock
    private PartyEntity partyEntity;
    @Mock
    private ClaimEntity claimEntity;
    @Mock
    private PcsCaseEntity pcsCaseEntity;

    @Captor
    private ArgumentCaptor<CounterClaimEntity> counterClaimCaptor;

    private CounterClaimService underTest;

    @BeforeEach
    void setUp() {
        underTest = new CounterClaimService(
            partyRepository,
            claimRepository,
            counterClaimRepository,
            counterClaimFeeCalculator,
            FIXED_UTC_CLOCK
        );
    }

    @Test
    void shouldSaveCounterClaimWithAllFields() {
        stubClaimRepository();
        when(claimEntity.getPcsCase()).thenReturn(pcsCaseEntity);
        when(counterClaimRepository.save(any(CounterClaimEntity.class))).thenAnswer(invocation -> {
            CounterClaimEntity entity = invocation.getArgument(0);
            entity.setId(COUNTER_CLAIM_ID);
            return entity;
        });

        CounterClaim counterClaim = CounterClaim.builder()
            .isClaimAmountKnown(VerticalYesNo.YES)
            .claimAmount(new BigDecimal("250.00"))
            .claimType(CounterClaimType.PAYMENT_OR_COMPENSATION)
            .counterClaimFor("Damage to property")
            .counterClaimReasons("Landlord failed to maintain property")
            .needHelpWithFees(VerticalYesNo.YES)
            .appliedForHwf(VerticalYesNo.NO)
            .build();

        Optional<CounterClaimEntity> saved = underTest.saveCounterClaim(CASE_REFERENCE, counterClaim, partyEntity);

        verify(counterClaimRepository).save(counterClaimCaptor.capture());
        CounterClaimEntity captured = counterClaimCaptor.getValue();
        assertThat(saved).contains(captured);
        assertThat(captured.getClaimType()).isEqualTo(CounterClaimType.PAYMENT_OR_COMPENSATION);
        assertThat(captured.getStatus()).isEqualTo(CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED);
        assertThat(captured.getParty()).isEqualTo(partyEntity);
        assertThat(captured.getPcsCase()).isEqualTo(pcsCaseEntity);
    }

    @ParameterizedTest
    @MethodSource("hwfStateScenarios")
    void shouldSetStateToPendingReviewWhenHwFReferenceProvided(boolean hwfReferenceProvided,
                                                                CounterClaimState expectedState) {
        // Given
        stubClaimRepository();

        CounterClaim counterClaim = mock(CounterClaim.class);
        when(counterClaimFeeCalculator.isHwfReferencePresent(counterClaim)).thenReturn(hwfReferenceProvided);

        when(counterClaimRepository.save(any(CounterClaimEntity.class))).thenReturn(mock(CounterClaimEntity.class));

        // When
        underTest.saveCounterClaim(CASE_REFERENCE, counterClaim, partyEntity);

        // Then
        verify(counterClaimRepository).save(counterClaimCaptor.capture());
        CounterClaimEntity savedCounterClaimEntity = counterClaimCaptor.getValue();

        assertThat(savedCounterClaimEntity.getStatus()).isEqualTo(expectedState);
    }

    private static Stream<Arguments> hwfStateScenarios() {
        return Stream.of(
            Arguments.argumentSet("HwF reference provided", true, CounterClaimState.PENDING_REVIEW),
            Arguments.argumentSet("HwF reference not provided", false, CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED)
        );
    }


    @Test
    void shouldReturnEmptyWhenCounterClaimIsNull() {
        assertThat(underTest.saveCounterClaim(CASE_REFERENCE, null, partyEntity)).isEmpty();
    }

    @Test
    void shouldSaveSomethingElseCounterClaimWithoutClaimAmountFields() {
        stubSaveDependencies();

        CounterClaim counterClaim = CounterClaim.builder()
            .claimType(CounterClaimType.SOMETHING_ELSE)
            .otherOrderRequestDetails("Stop eviction")
            .otherOrderRequestFacts("Landlord did not serve notice")
            .build();

        underTest.saveCounterClaim(CASE_REFERENCE, counterClaim, partyEntity);

        verify(counterClaimRepository).save(counterClaimCaptor.capture());
        CounterClaimEntity captured = counterClaimCaptor.getValue();
        assertThat(captured.getClaimType()).isEqualTo(CounterClaimType.SOMETHING_ELSE);
        assertThat(captured.getIsClaimAmountKnown()).isNull();
        assertThat(captured.getClaimAmount()).isNull();
        assertThat(captured.getOtherOrderRequestDetails()).isEqualTo("Stop eviction");
        assertThat(captured.getOtherOrderRequestFacts()).isEqualTo("Landlord did not serve notice");
    }

    @Test
    void shouldSaveCourtPermissionFieldsWhenPermissionGranted() {
        stubSaveDependencies();

        LocalDate permissionOrderDate = LocalDate.of(2026, 4, 1);
        LocalDate claimReceivedDate = LocalDate.of(2026, 4, 10);

        CounterClaim counterClaim = CounterClaim.builder()
            .claimType(CounterClaimType.PAYMENT_OR_COMPENSATION)
            .courtPermissionGranted(VerticalYesNo.YES)
            .permissionOrderDate(permissionOrderDate)
            .claimReceivedDate(claimReceivedDate)
            .build();

        underTest.saveCounterClaim(CASE_REFERENCE, counterClaim, partyEntity);

        verify(counterClaimRepository).save(counterClaimCaptor.capture());
        CounterClaimEntity captured = counterClaimCaptor.getValue();
        assertThat(captured.getCourtPermissionGranted()).isEqualTo(VerticalYesNo.YES);
        assertThat(captured.getPermissionOrderDate()).isEqualTo(permissionOrderDate);
        assertThat(captured.getClaimReceivedDate()).isEqualTo(claimReceivedDate);
    }

    @Test
    void shouldNullPermissionOrderDateWhenPermissionNotGranted() {
        stubSaveDependencies();

        LocalDate claimReceivedDate = LocalDate.of(2026, 4, 10);

        CounterClaim counterClaim = CounterClaim.builder()
            .claimType(CounterClaimType.PAYMENT_OR_COMPENSATION)
            .courtPermissionGranted(VerticalYesNo.NO)
            .permissionOrderDate(LocalDate.of(2026, 4, 1))
            .claimReceivedDate(claimReceivedDate)
            .build();

        underTest.saveCounterClaim(CASE_REFERENCE, counterClaim, partyEntity);

        verify(counterClaimRepository).save(counterClaimCaptor.capture());
        CounterClaimEntity captured = counterClaimCaptor.getValue();
        assertThat(captured.getCourtPermissionGranted()).isEqualTo(VerticalYesNo.NO);
        assertThat(captured.getPermissionOrderDate()).isNull();
        assertThat(captured.getClaimReceivedDate()).isEqualTo(claimReceivedDate);
    }

    @Test
    void shouldSaveCounterClaimAgainstParties() {
        UUID againstPartyId = UUID.randomUUID();
        PartyEntity againstParty = PartyEntity.builder().id(againstPartyId).build();
        stubSaveDependencies();
        when(partyRepository.getReferenceById(againstPartyId)).thenReturn(againstParty);

        CounterClaim counterClaim = CounterClaim.builder()
            .claimType(CounterClaimType.PAYMENT_OR_COMPENSATION)
            .counterClaimAgainst(List.of(
                ListValue.<Party>builder().id(againstPartyId.toString()).value(Party.builder().build()).build()
            ))
            .build();

        underTest.saveCounterClaim(CASE_REFERENCE, counterClaim, partyEntity);

        verify(counterClaimRepository).save(counterClaimCaptor.capture());
        assertThat(counterClaimCaptor.getValue().getCounterClaimParties()).hasSize(1);
    }

    @Test
    void shouldThrowWhenPartyIsNull() {
        CounterClaim counterClaim = CounterClaim.builder()
            .claimType(CounterClaimType.PAYMENT_OR_COMPENSATION)
            .build();

        assertThatThrownBy(() -> underTest.saveCounterClaim(CASE_REFERENCE, counterClaim, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("party is null for case: " + CASE_REFERENCE);
    }

    @Test
    void shouldThrowWhenClaimNotFoundForCase() {
        when(claimRepository.findIdByCaseReference(CASE_REFERENCE)).thenReturn(Optional.empty());

        CounterClaim counterClaim = CounterClaim.builder()
            .claimType(CounterClaimType.PAYMENT_OR_COMPENSATION)
            .build();

        assertThatThrownBy(() -> underTest.saveCounterClaim(CASE_REFERENCE, counterClaim, partyEntity))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No claim found for case");
    }

    private void stubSaveDependencies() {
        stubClaimRepository();
        when(claimEntity.getPcsCase()).thenReturn(pcsCaseEntity);
        when(counterClaimRepository.save(any(CounterClaimEntity.class))).thenAnswer(invocation -> {
            CounterClaimEntity entity = invocation.getArgument(0);
            entity.setId(COUNTER_CLAIM_ID);
            return entity;
        });
    }

    private void stubClaimRepository() {
        when(claimRepository.findIdByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(CLAIM_ID));
        when(claimRepository.getReferenceById(CLAIM_ID)).thenReturn(claimEntity);
    }
}
