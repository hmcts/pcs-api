package uk.gov.hmcts.reform.pcs.enforcement.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.entity.BailiffAllocationEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WarrantEntity;
import uk.gov.hmcts.reform.pcs.enforcement.exception.EnforcementEventNotAvailableException;
import uk.gov.hmcts.reform.pcs.enforcement.exception.NoEnforcementException;
import uk.gov.hmcts.reform.pcs.enforcement.repository.BailiffAllocationRepository;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_BAILIFF_ALLOCATION;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_SCHEDULING;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLAIMANT_CONFIRMED;

@ExtendWith(MockitoExtension.class)
class BailiffAllocationServiceTest {

    private static final long CASE_REFERENCE = 1234L;
    private static final Instant NOW = Instant.parse("2026-02-01T16:20:00Z");
    private static final BailiffUser SAM = new BailiffUser("sam-id", "Sam Jones", "sam.jones@justice.gov.uk");

    @Mock
    private CurrentEnforcementService currentEnforcementService;
    @Mock
    private BailiffAllocationRepository bailiffAllocationRepository;
    @Mock
    private EnforcementOrderRepository enforcementOrderRepository;
    @Mock
    private SecurityContextService securityContextService;

    private BailiffAllocationService underTest;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        underTest = new BailiffAllocationService(currentEnforcementService, new EnforcementStatusTransitions(clock),
                                                 bailiffAllocationRepository, enforcementOrderRepository,
                                                 securityContextService, clock);
    }

    @Test
    void shouldAllocateAWarrantAwaitingAllocationAndMoveItToAwaitingScheduling() {
        EnforcementOrderEntity enforcement = givenCurrentEnforcement(AWAITING_BAILIFF_ALLOCATION);
        givenActiveAllocations(enforcement);
        when(securityContextService.getCurrentUserId()).thenReturn("manager-id");

        underTest.allocate(CASE_REFERENCE, SAM);

        BailiffAllocationEntity saved = savedAllocation();
        assertThat(saved.getEnforcementOrder()).isSameAs(enforcement);
        assertThat(saved.getBailiffUserId()).isEqualTo("sam-id");
        assertThat(saved.getBailiffName()).isEqualTo("Sam Jones");
        assertThat(saved.getBailiffEmail()).isEqualTo("sam.jones@justice.gov.uk");
        assertThat(saved.getAllocatedByUserId()).isEqualTo("manager-id");
        assertThat(saved.getStartedAt()).isEqualTo(NOW);
        assertThat(saved.getEndedAt()).isNull();
        assertThat(enforcement.getStatus()).isEqualTo(AWAITING_SCHEDULING);
        assertThat(enforcement.getStatusUpdatedAt()).isEqualTo(NOW);
        verify(enforcementOrderRepository).save(enforcement);
    }

    @Test
    void shouldEndThePreviousAllocationWhenReallocatingWithoutChangingTheStatus() {
        EnforcementOrderEntity enforcement = givenCurrentEnforcement(CLAIMANT_CONFIRMED);
        BailiffAllocationEntity previous = allocation(enforcement, "alex-id");
        givenActiveAllocations(enforcement, previous);

        underTest.allocate(CASE_REFERENCE, SAM);

        assertThat(previous.getEndedAt()).isEqualTo(NOW);
        verify(bailiffAllocationRepository).saveAllAndFlush(List.of(previous));
        assertThat(savedAllocation().getBailiffUserId()).isEqualTo("sam-id");
        assertThat(enforcement.getStatus()).isEqualTo(CLAIMANT_CONFIRMED);
        verify(enforcementOrderRepository, never()).save(any());
    }

    @Test
    void shouldChangeNothingWhenAllocatingToTheCurrentBailiff() {
        EnforcementOrderEntity enforcement = givenCurrentEnforcement(AWAITING_SCHEDULING);
        BailiffAllocationEntity current = allocation(enforcement, "sam-id");
        givenActiveAllocations(enforcement, current);

        underTest.allocate(CASE_REFERENCE, SAM);

        assertThat(current.getEndedAt()).isNull();
        verify(bailiffAllocationRepository, never()).save(any());
        assertThat(enforcement.getStatus()).isEqualTo(AWAITING_SCHEDULING);
    }

    @Test
    void shouldRefuseWhenTheClaimHasNoEnforcement() {
        when(currentEnforcementService.findCurrentEnforcement(CASE_REFERENCE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.allocate(CASE_REFERENCE, SAM)).isInstanceOf(NoEnforcementException.class);
        verify(bailiffAllocationRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = EnforcementStatus.class, names = {"AWAITING_PAYMENT", "AWAITING_REVIEW", "CLOSED", "REJECTED"})
    void shouldRefuseAWarrantThatIsNotApprovedOrNotLive(EnforcementStatus status) {
        givenCurrentEnforcement(status);

        assertThatThrownBy(() -> underTest.allocate(CASE_REFERENCE, SAM))
            .isInstanceOf(EnforcementEventNotAvailableException.class)
            .hasMessage(BailiffAllocationService.NOT_AVAILABLE_MESSAGE);
        verify(bailiffAllocationRepository, never()).save(any());
    }

    @Test
    void shouldRefuseAWrit() {
        EnforcementOrderEntity writ = EnforcementOrderEntity.builder().id(UUID.randomUUID())
            .status(AWAITING_BAILIFF_ALLOCATION).build();
        when(currentEnforcementService.findCurrentEnforcement(CASE_REFERENCE)).thenReturn(Optional.of(writ));

        assertThatThrownBy(() -> underTest.getAllocatableEnforcement(CASE_REFERENCE))
            .isInstanceOf(EnforcementEventNotAvailableException.class);
    }

    @Test
    void shouldFindTheActiveAllocation() {
        EnforcementOrderEntity enforcement = EnforcementOrderEntity.builder().id(UUID.randomUUID()).build();
        BailiffAllocationEntity current = allocation(enforcement, "sam-id");
        givenActiveAllocations(enforcement, current);

        assertThat(underTest.findActiveAllocation(enforcement)).contains(current);
    }

    private EnforcementOrderEntity givenCurrentEnforcement(EnforcementStatus status) {
        EnforcementOrderEntity enforcement = EnforcementOrderEntity.builder()
            .id(UUID.randomUUID())
            .warrantDetails(new WarrantEntity())
            .status(status)
            .build();
        when(currentEnforcementService.findCurrentEnforcement(CASE_REFERENCE)).thenReturn(Optional.of(enforcement));
        return enforcement;
    }

    private void givenActiveAllocations(EnforcementOrderEntity enforcement, BailiffAllocationEntity... active) {
        when(bailiffAllocationRepository.findByEnforcementOrderIdAndEndedAtIsNull(enforcement.getId()))
            .thenReturn(new ArrayList<>(List.of(active)));
    }

    private static BailiffAllocationEntity allocation(EnforcementOrderEntity enforcement, String userId) {
        return BailiffAllocationEntity.builder()
            .id(UUID.randomUUID())
            .enforcementOrder(enforcement)
            .bailiffUserId(userId)
            .startedAt(NOW.minusSeconds(3600))
            .build();
    }

    private BailiffAllocationEntity savedAllocation() {
        ArgumentCaptor<BailiffAllocationEntity> captor = ArgumentCaptor.forClass(BailiffAllocationEntity.class);
        verify(bailiffAllocationRepository).save(captor.capture());
        return captor.getValue();
    }
}
