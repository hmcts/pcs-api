package uk.gov.hmcts.reform.pcs.enforcement.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
    @Mock
    private EnforcementWaTaskService enforcementWaTaskService;

    private BailiffAllocationService underTest;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        underTest = new BailiffAllocationService(currentEnforcementService, new EnforcementStatusTransitions(clock),
                                                 bailiffAllocationRepository, enforcementOrderRepository,
                                                 securityContextService, enforcementWaTaskService, clock);
    }

    @Test
    void shouldAllocateAWarrantAwaitingAllocationAndMoveItToAwaitingScheduling() {
        EnforcementOrderEntity enforcement = givenCurrentEnforcement(AWAITING_BAILIFF_ALLOCATION);
        givenActiveAllocations(enforcement);
        givenSaveAssignsAnId();
        when(securityContextService.getCurrentUserId()).thenReturn("manager-id");

        EnforcementChange<Void> allocated = underTest.allocate(CASE_REFERENCE, SAM);

        assertThat(allocated.refused()).isFalse();
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
        verify(enforcementWaTaskService).bailiffAllocated(CASE_REFERENCE, saved.getId());
    }

    @Test
    void shouldEndThePreviousAllocationWhenReallocatingWithoutChangingTheStatus() {
        EnforcementOrderEntity enforcement = givenCurrentEnforcement(CLAIMANT_CONFIRMED);
        BailiffAllocationEntity previous = allocation(enforcement, "alex-id");
        givenActiveAllocations(enforcement, previous);
        givenSaveAssignsAnId();

        underTest.allocate(CASE_REFERENCE, SAM);

        assertThat(previous.getEndedAt()).isEqualTo(NOW);
        verify(bailiffAllocationRepository).saveAllAndFlush(List.of(previous));
        BailiffAllocationEntity saved = savedAllocation();
        assertThat(saved.getBailiffUserId()).isEqualTo("sam-id");
        assertThat(enforcement.getStatus()).isEqualTo(CLAIMANT_CONFIRMED);
        verify(enforcementOrderRepository, never()).save(any());
    }

    /**
     * The previous bailiff's "Review risks and set level" task is still open and still the one to do, and the
     * allocated-bailiff role assignment is what moves it. A second request would leave the new bailiff looking
     * at two identical tasks.
     */
    @Test
    void shouldNotAskForASecondRiskTaskWhenReallocating() {
        EnforcementOrderEntity enforcement = givenCurrentEnforcement(CLAIMANT_CONFIRMED);
        givenActiveAllocations(enforcement, allocation(enforcement, "alex-id"));
        givenSaveAssignsAnId();

        EnforcementChange<Void> allocated = underTest.allocate(CASE_REFERENCE, SAM);

        assertThat(allocated.refused()).isFalse();
        verifyNoInteractions(enforcementWaTaskService);
    }

    /**
     * A warrant a return reopened for allocation has no bailiff, so the new one is asked for the risk level
     * only if it is not recorded already.
     */
    @Test
    void shouldNotAskForTheRiskTaskWhenTheRiskLevelIsAlreadyRecorded() {
        EnforcementOrderEntity enforcement = givenCurrentEnforcement(AWAITING_BAILIFF_ALLOCATION);
        enforcement.setRiskLevelSetAt(NOW.minusSeconds(86400));
        givenActiveAllocations(enforcement);
        givenSaveAssignsAnId();

        EnforcementChange<Void> allocated = underTest.allocate(CASE_REFERENCE, SAM);

        assertThat(allocated.refused()).isFalse();
        assertThat(enforcement.getStatus()).isEqualTo(AWAITING_SCHEDULING);
        verifyNoInteractions(enforcementWaTaskService);
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
        verifyNoInteractions(enforcementWaTaskService);
    }

    /**
     * The caseworker is asked to try again rather than given an unexplained 500, and the allocation is rolled
     * back with the task request.
     */
    @Test
    void shouldRefuseTheAllocationWhenTheRiskTaskCannotBeRequested() {
        EnforcementOrderEntity enforcement = givenCurrentEnforcement(AWAITING_BAILIFF_ALLOCATION);
        givenActiveAllocations(enforcement);
        givenSaveAssignsAnId();
        doThrow(new DataIntegrityViolationException("scheduled_tasks"))
            .when(enforcementWaTaskService).bailiffAllocated(anyLong(), any());

        EnforcementChange<Void> allocated = underTest.allocate(CASE_REFERENCE, SAM);

        assertThat(allocated.errors()).containsExactly(EnforcementSideEffects.COULD_NOT_SCHEDULE_ERROR);
        assertThat(enforcement.getStatus()).isEqualTo(AWAITING_BAILIFF_ALLOCATION);
        verify(enforcementOrderRepository, never()).save(any());
    }

    @Test
    void shouldRefuseWhenTheClaimHasNoEnforcement() {
        when(currentEnforcementService.findCurrentEnforcement(CASE_REFERENCE)).thenReturn(Optional.empty());

        EnforcementChange<Void> allocated = underTest.allocate(CASE_REFERENCE, SAM);

        assertThat(allocated.errors()).containsExactly(NoEnforcementException.MESSAGE);
        verify(bailiffAllocationRepository, never()).save(any());
        verifyNoInteractions(enforcementWaTaskService);
    }

    @ParameterizedTest
    @EnumSource(value = EnforcementStatus.class, names = {"AWAITING_PAYMENT", "AWAITING_REVIEW", "CLOSED", "REJECTED"})
    void shouldRefuseAWarrantThatIsNotApprovedOrNotLive(EnforcementStatus status) {
        givenCurrentEnforcement(status);

        EnforcementChange<Void> allocated = underTest.allocate(CASE_REFERENCE, SAM);

        assertThat(allocated.errors()).containsExactly(BailiffAllocationService.NOT_AVAILABLE_MESSAGE);
        verify(bailiffAllocationRepository, never()).save(any());
        verifyNoInteractions(enforcementWaTaskService);
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

    /**
     * The allocation's id keys the risk task, and the database assigns it on save.
     */
    private void givenSaveAssignsAnId() {
        when(bailiffAllocationRepository.save(any())).thenAnswer(invocation -> {
            BailiffAllocationEntity allocation = invocation.getArgument(0);
            allocation.setId(UUID.randomUUID());
            return allocation;
        });
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
