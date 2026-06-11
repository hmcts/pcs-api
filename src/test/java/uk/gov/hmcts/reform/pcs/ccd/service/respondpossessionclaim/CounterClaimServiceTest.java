package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CounterClaimServiceTest {

    private static final long CASE_REFERENCE = 1234567890L;
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID PARTY_ID = UUID.randomUUID();
    private static final UUID CLAIM_ID = UUID.randomUUID();
    private static final UUID COUNTER_CLAIM_ID = UUID.randomUUID();
    private static final Clock FIXED_UTC_CLOCK = Clock.fixed(
        Instant.parse("2026-04-22T21:00:00Z"), ZoneOffset.UTC);

    @Mock
    private PartyService partyService;
    @Mock
    private PartyRepository partyRepository;
    @Mock
    private ClaimRepository claimRepository;
    @Mock
    private CounterClaimRepository counterClaimRepository;
    @Mock
    private SecurityContextService securityContextService;
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
            partyService,
            partyRepository,
            claimRepository,
            counterClaimRepository,
            securityContextService,
            FIXED_UTC_CLOCK
        );
    }

    @Test
    void shouldSaveCounterClaimWithAllFields() {
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(partyService.getPartyEntityByIdamId(USER_ID, CASE_REFERENCE)).thenReturn(partyEntity);
        when(partyEntity.getId()).thenReturn(PARTY_ID);
        when(partyRepository.getReferenceById(PARTY_ID)).thenReturn(partyEntity);
        when(claimRepository.findIdByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(CLAIM_ID));
        when(claimRepository.getReferenceById(CLAIM_ID)).thenReturn(claimEntity);
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

        Optional<CounterClaimEntity> saved = underTest.saveCounterClaim(CASE_REFERENCE, counterClaim);

        verify(counterClaimRepository).save(counterClaimCaptor.capture());
        CounterClaimEntity captured = counterClaimCaptor.getValue();
        assertThat(saved).contains(captured);
        assertThat(captured.getClaimType()).isEqualTo(CounterClaimType.PAYMENT_OR_COMPENSATION);
        assertThat(captured.getStatus()).isEqualTo(CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED);
        assertThat(captured.getParty()).isEqualTo(partyEntity);
        assertThat(captured.getPcsCase()).isEqualTo(pcsCaseEntity);
    }

    @Test
    void shouldReturnEmptyWhenCounterClaimIsNull() {
        assertThat(underTest.saveCounterClaim(CASE_REFERENCE, null)).isEmpty();
    }

    @Test
    void shouldIssueCounterClaim() {
        CounterClaimEntity pendingCounterClaim = CounterClaimEntity.builder()
            .id(COUNTER_CLAIM_ID)
            .status(CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED)
            .build();

        when(counterClaimRepository.save(pendingCounterClaim)).thenAnswer(invocation -> invocation.getArgument(0));

        CounterClaimEntity issued = underTest.issueCounterClaim(pendingCounterClaim);

        assertThat(issued.getStatus()).isEqualTo(CounterClaimState.COUNTER_CLAIM_ISSUED);
        assertThat(issued.getClaimIssuedDate()).isEqualTo(LocalDateTime.of(2026, 4, 22, 21, 0));
        verify(counterClaimRepository).save(pendingCounterClaim);
    }

    @Test
    void shouldSaveSomethingElseCounterClaimWithoutClaimAmountFields() {
        stubSaveDependencies();

        CounterClaim counterClaim = CounterClaim.builder()
            .claimType(CounterClaimType.SOMETHING_ELSE)
            .otherOrderRequestDetails("Stop eviction")
            .otherOrderRequestFacts("Landlord did not serve notice")
            .build();

        underTest.saveCounterClaim(CASE_REFERENCE, counterClaim);

        verify(counterClaimRepository).save(counterClaimCaptor.capture());
        CounterClaimEntity captured = counterClaimCaptor.getValue();
        assertThat(captured.getClaimType()).isEqualTo(CounterClaimType.SOMETHING_ELSE);
        assertThat(captured.getIsClaimAmountKnown()).isNull();
        assertThat(captured.getClaimAmount()).isNull();
        assertThat(captured.getOtherOrderRequestDetails()).isEqualTo("Stop eviction");
        assertThat(captured.getOtherOrderRequestFacts()).isEqualTo("Landlord did not serve notice");
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

        underTest.saveCounterClaim(CASE_REFERENCE, counterClaim);

        verify(counterClaimRepository).save(counterClaimCaptor.capture());
        assertThat(counterClaimCaptor.getValue().getCounterClaimParties()).hasSize(1);
    }

    @Test
    void shouldThrowWhenCurrentUserIdIsNull() {
        when(securityContextService.getCurrentUserId()).thenReturn(null);

        CounterClaim counterClaim = CounterClaim.builder()
            .claimType(CounterClaimType.PAYMENT_OR_COMPENSATION)
            .build();

        assertThatThrownBy(() -> underTest.saveCounterClaim(CASE_REFERENCE, counterClaim))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Current user IDAM ID is null");
    }

    @Test
    void shouldThrowWhenClaimNotFoundForCase() {
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(partyService.getPartyEntityByIdamId(USER_ID, CASE_REFERENCE)).thenReturn(partyEntity);
        when(partyEntity.getId()).thenReturn(PARTY_ID);
        when(partyRepository.getReferenceById(PARTY_ID)).thenReturn(partyEntity);
        when(claimRepository.findIdByCaseReference(CASE_REFERENCE)).thenReturn(Optional.empty());

        CounterClaim counterClaim = CounterClaim.builder()
            .claimType(CounterClaimType.PAYMENT_OR_COMPENSATION)
            .build();

        assertThatThrownBy(() -> underTest.saveCounterClaim(CASE_REFERENCE, counterClaim))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No claim found for case");
    }

    private void stubSaveDependencies() {
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(partyService.getPartyEntityByIdamId(USER_ID, CASE_REFERENCE)).thenReturn(partyEntity);
        when(partyEntity.getId()).thenReturn(PARTY_ID);
        when(partyRepository.getReferenceById(PARTY_ID)).thenReturn(partyEntity);
        when(claimRepository.findIdByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(CLAIM_ID));
        when(claimRepository.getReferenceById(CLAIM_ID)).thenReturn(claimEntity);
        when(claimEntity.getPcsCase()).thenReturn(pcsCaseEntity);
        when(counterClaimRepository.save(any(CounterClaimEntity.class))).thenAnswer(invocation -> {
            CounterClaimEntity entity = invocation.getArgument(0);
            entity.setId(COUNTER_CLAIM_ID);
            return entity;
        });
    }
}
