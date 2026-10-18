package uk.gov.hmcts.reform.pcs.enforcement.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.InterimWarrantReturnName;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.SuccessfulWarrantReturnName;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.UnsuccessfulWarrantReturnName;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnName;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnOutcome;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnType;
import uk.gov.hmcts.reform.pcs.enforcement.entity.BailiffAllocationEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WarrantReturnEntity;
import uk.gov.hmcts.reform.pcs.enforcement.exception.NoEnforcementException;
import uk.gov.hmcts.reform.pcs.enforcement.exception.EnforcementEventNotAvailableException;
import uk.gov.hmcts.reform.pcs.enforcement.repository.BailiffAllocationRepository;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.enforcement.repository.WarrantReturnRepository;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_BAILIFF_ALLOCATION;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_SCHEDULING;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.BREATHING_SPACE;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLAIMANT_CONFIRMED;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLOSED;

@ExtendWith(MockitoExtension.class)
class WarrantReturnServiceTest {

    private static final long CASE_REFERENCE = 1234L;
    private static final Instant NOW = Instant.parse("2026-07-08T09:00:00Z");
    private static final String USER_ID = "bailiff-uid";

    @Mock
    private CurrentEnforcementService currentEnforcementService;
    @Mock
    private WarrantReturnRepository warrantReturnRepository;
    @Mock
    private SecurityContextService securityContextService;
    @Mock
    private BailiffAllocationRepository bailiffAllocationRepository;
    @Mock
    private EnforcementOrderRepository enforcementOrderRepository;

    private WarrantReturnService underTest;

    @BeforeEach
    void setUp() {
        Clock utcClock = Clock.fixed(NOW, ZoneOffset.UTC);
        EnforcementStatusTransitions transitions = new EnforcementStatusTransitions(utcClock);
        underTest = new WarrantReturnService(
            currentEnforcementService,
            warrantReturnRepository,
            transitions,
            new WarrantReturnValidator(Clock.fixed(NOW, UK_ZONE_ID)),
            securityContextService,
            new BailiffAllocationService(currentEnforcementService, transitions, bailiffAllocationRepository,
                                         enforcementOrderRepository, securityContextService, utcClock),
            utcClock);
    }

    @Test
    void shouldFindTheWarrant() {
        EnforcementOrderEntity warrant = enforcement(SelectEnforcementType.WARRANT, CLAIMANT_CONFIRMED);
        when(currentEnforcementService.findCurrentEnforcement(CASE_REFERENCE)).thenReturn(Optional.of(warrant));

        assertThat(underTest.findWarrant(CASE_REFERENCE)).isSameAs(warrant);
    }

    @ParameterizedTest
    @EnumSource(value = EnforcementStatus.class, names = {
        "AWAITING_SCHEDULING", "AWAITING_CLAIMANT_CONFIRMATION", "CLAIMANT_CONFIRMED", "CLAIMANT_UNABLE_TO_ATTEND",
        "AWAITING_PROPERTY_VISIT", "BREATHING_SPACE", "CLOSED"})
    void shouldTakeAReturnOnAWarrantOfRestitutionOnceABailiffIsAllocated(EnforcementStatus status) {
        EnforcementOrderEntity warrant = enforcement(SelectEnforcementType.WARRANT_OF_RESTITUTION, status);
        when(currentEnforcementService.findCurrentEnforcement(CASE_REFERENCE)).thenReturn(Optional.of(warrant));

        assertThat(underTest.findWarrant(CASE_REFERENCE)).isSameAs(warrant);
    }

    @Test
    void shouldRefuseWhenThereIsNoEnforcement() {
        when(currentEnforcementService.findCurrentEnforcement(CASE_REFERENCE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.findWarrant(CASE_REFERENCE))
            .isInstanceOf(NoEnforcementException.class)
            .hasMessage("There is no warrant on this case");
    }

    @ParameterizedTest
    @EnumSource(value = SelectEnforcementType.class, names = {"WRIT", "WRIT_OF_RESTITUTION"})
    void shouldRefuseAWrit(SelectEnforcementType type) {
        when(currentEnforcementService.findCurrentEnforcement(CASE_REFERENCE))
            .thenReturn(Optional.of(enforcement(type, CLAIMANT_CONFIRMED)));

        assertThatThrownBy(() -> underTest.findWarrant(CASE_REFERENCE)).isInstanceOf(NoEnforcementException.class);
    }

    @ParameterizedTest
    @EnumSource(value = EnforcementStatus.class, names = {
        "AWAITING_PAYMENT", "AWAITING_REVIEW", "REJECTED", "TRANSFERRED_TO_HIGH_COURT"})
    void shouldRefuseAWarrantThatHasNotBeenGenerated(EnforcementStatus status) {
        when(currentEnforcementService.findCurrentEnforcement(CASE_REFERENCE))
            .thenReturn(Optional.of(enforcement(SelectEnforcementType.WARRANT, status)));

        assertThatThrownBy(() -> underTest.findWarrant(CASE_REFERENCE)).isInstanceOf(NoEnforcementException.class);
    }

    @Test
    void shouldRefuseAWarrantWithNoBailiffAllocated() {
        when(currentEnforcementService.findCurrentEnforcement(CASE_REFERENCE))
            .thenReturn(Optional.of(enforcement(SelectEnforcementType.WARRANT, AWAITING_BAILIFF_ALLOCATION)));

        assertThatThrownBy(() -> underTest.findWarrant(CASE_REFERENCE))
            .isInstanceOf(EnforcementEventNotAvailableException.class)
            .hasMessage("A bailiff must be allocated to the warrant before a return can be recorded");
    }

    @Test
    void shouldRecogniseAWarrantFromTheLegacyEnforcementTypeField() {
        EnforcementOrderEntity warrant = new EnforcementOrderEntity();
        warrant.setStatus(CLAIMANT_CONFIRMED);
        warrant.setEnforcementOrder(EnforcementOrder.builder()
                                        .selectEnforcementType(SelectEnforcementType.WARRANT).build());

        assertThat(WarrantReturnService.isWarrant(warrant)).isTrue();
        warrant.setEnforcementOrder(null);
        assertThat(WarrantReturnService.isWarrant(warrant)).isFalse();
    }

    @Test
    void shouldValidateAgainstTheWarrantStatus() {
        when(currentEnforcementService.findCurrentEnforcement(CASE_REFERENCE))
            .thenReturn(Optional.of(enforcement(SelectEnforcementType.WARRANT, CLOSED)));

        assertThat(underTest.validate(CASE_REFERENCE, interim(InterimWarrantReturnName.CROWDS_OR_PROTESTS, null)))
            .containsExactly("The warrant is closed. The only return you can record is \"Reopen warrant\"");
    }

    @Test
    void shouldRecordASuccessfulEvictionAndCloseTheWarrant() {
        EnforcementOrderEntity warrant = givenWarrant(CLAIMANT_CONFIRMED);
        warrant.setEnforcementNumber("WP-00000042");
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        LocalDateTime evictedAt = LocalDateTime.of(2026, 7, 8, 9, 15);

        EnforcementChange<RecordedWarrantReturn> recorded =
            underTest.recordReturn(CASE_REFERENCE, WarrantReturnDetails.builder()
                .type(WarrantReturnType.FINAL)
                .outcome(WarrantReturnOutcome.EVICTION_SUCCESSFUL)
                .successfulReturnName(SuccessfulWarrantReturnName.EVICTION_SUCCESSFUL)
                .unsuccessfulReturnName(UnsuccessfulWarrantReturnName.WARRANT_EXPIRED)
                .evictionDateTime(evictedAt)
                .additionalDetails("Keys handed over")
                .build());

        assertThat(recorded.change()).isEqualTo(
            new RecordedWarrantReturn(WarrantReturnName.EVICTION_SUCCESSFUL, "WP-00000042"));
        WarrantReturnEntity saved = savedReturn();
        assertThat(saved.getEnforcementOrder()).isSameAs(warrant);
        assertThat(saved.getReturnType()).isEqualTo(WarrantReturnType.FINAL);
        assertThat(saved.getOutcome()).isEqualTo(WarrantReturnOutcome.EVICTION_SUCCESSFUL);
        assertThat(saved.getReturnName()).isEqualTo(WarrantReturnName.EVICTION_SUCCESSFUL);
        // 09:15 BST
        assertThat(saved.getEvictionAt()).isEqualTo(Instant.parse("2026-07-08T08:15:00Z"));
        assertThat(saved.getAdditionalDetails()).isEqualTo("Keys handed over");
        assertThat(saved.getRecordedAt()).isEqualTo(NOW);
        assertThat(saved.getRecordedBy()).isEqualTo(USER_ID);
        assertThat(warrant.getStatus()).isEqualTo(CLOSED);
        assertThat(warrant.getStatusUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldRecordAnInterimReturnWithoutAnOutcomeOrEvictionTime() {
        final EnforcementOrderEntity warrant = givenWarrant(CLAIMANT_CONFIRMED);

        underTest.recordReturn(CASE_REFERENCE, WarrantReturnDetails.builder()
            .type(WarrantReturnType.INTERIM)
            .outcome(WarrantReturnOutcome.EVICTION_SUCCESSFUL)
            .evictionDateTime(LocalDateTime.of(2026, 7, 8, 9, 15))
            .interimReturnName(InterimWarrantReturnName.OBSTRUCTION_BY_DEFENDANTS)
            .additionalDetails("   ")
            .build());

        WarrantReturnEntity saved = savedReturn();
        assertThat(saved.getReturnType()).isEqualTo(WarrantReturnType.INTERIM);
        assertThat(saved.getOutcome()).isNull();
        assertThat(saved.getEvictionAt()).isNull();
        assertThat(saved.getAdditionalDetails()).isNull();
        assertThat(saved.getReturnName()).isEqualTo(WarrantReturnName.OBSTRUCTION_BY_DEFENDANTS);
        assertThat(warrant.getStatus()).isEqualTo(AWAITING_SCHEDULING);
    }

    @Test
    void shouldLeaveTheStatusUnchangedForAnUnchangedReturnName() {
        EnforcementOrderEntity warrant = givenWarrant(CLAIMANT_CONFIRMED);
        Instant before = warrant.getStatusUpdatedAt();

        underTest.recordReturn(CASE_REFERENCE, interim(InterimWarrantReturnName.OTHER_INTERIM_RETURN, "Court order"));

        assertThat(warrant.getStatus()).isEqualTo(CLAIMANT_CONFIRMED);
        assertThat(warrant.getStatusUpdatedAt()).isEqualTo(before);
        verify(warrantReturnRepository).save(any());
    }

    @Test
    void shouldRecordABreathingSpaceReturnOnAWarrantAlreadyInBreathingSpace() {
        EnforcementOrderEntity warrant = givenWarrant(BREATHING_SPACE);
        Instant before = warrant.getStatusUpdatedAt();

        underTest.recordReturn(CASE_REFERENCE,
                               interim(InterimWarrantReturnName.CLAIMANT_ADVISED_BREATHING_SPACE, null));

        assertThat(warrant.getStatus()).isEqualTo(BREATHING_SPACE);
        assertThat(warrant.getStatusUpdatedAt()).isEqualTo(before);
        verify(warrantReturnRepository).save(any());
    }

    @Test
    void shouldReopenAClosedWarrant() {
        EnforcementOrderEntity warrant = givenWarrant(CLOSED);

        underTest.recordReturn(CASE_REFERENCE, interim(InterimWarrantReturnName.REOPEN_WARRANT, "Suspension lifted"));

        assertThat(warrant.getStatus()).isEqualTo(AWAITING_BAILIFF_ALLOCATION);
    }

    @Test
    void shouldEndTheBailiffAllocationWhenAFinalReturnClosesTheWarrant() {
        EnforcementOrderEntity warrant = givenWarrant(CLAIMANT_CONFIRMED);
        BailiffAllocationEntity allocation = givenActiveAllocation(warrant);

        underTest.recordReturn(CASE_REFERENCE, WarrantReturnDetails.builder()
            .type(WarrantReturnType.FINAL)
            .outcome(WarrantReturnOutcome.EVICTION_UNSUCCESSFUL)
            .unsuccessfulReturnName(UnsuccessfulWarrantReturnName.WARRANT_EXPIRED)
            .build());

        assertThat(warrant.getStatus()).isEqualTo(CLOSED);
        assertThat(allocation.getEndedAt()).isEqualTo(NOW);
        verify(bailiffAllocationRepository).saveAllAndFlush(List.of(allocation));
    }

    @Test
    void shouldLeaveAReopenedWarrantWithNoActiveBailiffAllocation() {
        EnforcementOrderEntity warrant = givenWarrant(CLOSED);
        BailiffAllocationEntity allocation = givenActiveAllocation(warrant);

        underTest.recordReturn(CASE_REFERENCE, interim(InterimWarrantReturnName.REOPEN_WARRANT, "Suspension lifted"));

        assertThat(warrant.getStatus()).isEqualTo(AWAITING_BAILIFF_ALLOCATION);
        assertThat(allocation.getEndedAt()).isEqualTo(NOW);
        verify(bailiffAllocationRepository).saveAllAndFlush(List.of(allocation));
    }

    @Test
    void shouldKeepTheBailiffAllocationForAReturnThatLeavesTheWarrantLive() {
        givenWarrant(CLAIMANT_CONFIRMED);

        underTest.recordReturn(CASE_REFERENCE,
                               interim(InterimWarrantReturnName.OBSTRUCTION_BY_DEFENDANTS, null));

        verifyNoInteractions(bailiffAllocationRepository);
    }

    @Test
    void shouldNotRecordAnInvalidReturn() {
        EnforcementOrderEntity warrant = givenWarrant(CLAIMANT_CONFIRMED);

        EnforcementChange<RecordedWarrantReturn> recorded = underTest.recordReturn(
            CASE_REFERENCE, interim(InterimWarrantReturnName.OTHER_INTERIM_RETURN, null));

        assertThat(recorded.errors()).containsExactly("Enter additional details");
        verify(warrantReturnRepository, never()).save(any());
        assertThat(warrant.getStatus()).isEqualTo(CLAIMANT_CONFIRMED);
    }

    @Test
    void shouldRefuseRatherThanThrowWhenThereIsNoWarrantToRecordAReturnAgainst() {
        when(currentEnforcementService.findCurrentEnforcement(CASE_REFERENCE)).thenReturn(Optional.empty());

        EnforcementChange<RecordedWarrantReturn> recorded = underTest.recordReturn(
            CASE_REFERENCE, interim(InterimWarrantReturnName.OTHER_INTERIM_RETURN, "Court order"));

        assertThat(recorded.errors()).containsExactly(NoEnforcementException.MESSAGE);
        verify(warrantReturnRepository, never()).save(any());
    }

    @Test
    void shouldRefuseRatherThanThrowWhenNoBailiffIsAllocatedYet() {
        givenWarrant(AWAITING_BAILIFF_ALLOCATION);

        EnforcementChange<RecordedWarrantReturn> recorded = underTest.recordReturn(
            CASE_REFERENCE, interim(InterimWarrantReturnName.OTHER_INTERIM_RETURN, "Court order"));

        assertThat(recorded.errors()).containsExactly(WarrantReturnService.NOT_ALLOCATED_MESSAGE);
        verify(warrantReturnRepository, never()).save(any());
    }

    @Test
    void shouldFindTheLatestReturnOfTheCurrentEnforcement() {
        PcsCaseEntity pcsCase = new PcsCaseEntity();
        EnforcementOrderEntity warrant = enforcement(SelectEnforcementType.WARRANT, CLOSED);
        WarrantReturnEntity latest = new WarrantReturnEntity();
        when(currentEnforcementService.findCurrentEnforcement(pcsCase)).thenReturn(Optional.of(warrant));
        when(warrantReturnRepository.findFirstByEnforcementOrderIdOrderByRecordedAtDesc(warrant.getId()))
            .thenReturn(Optional.of(latest));

        assertThat(underTest.findLatestReturn(pcsCase)).containsSame(latest);
    }

    @Test
    void shouldFindNoLatestReturnWithoutAnEnforcement() {
        PcsCaseEntity pcsCase = new PcsCaseEntity();
        when(currentEnforcementService.findCurrentEnforcement(pcsCase)).thenReturn(Optional.empty());

        assertThat(underTest.findLatestReturn(pcsCase)).isEmpty();
    }

    private EnforcementOrderEntity givenWarrant(EnforcementStatus status) {
        EnforcementOrderEntity warrant = enforcement(SelectEnforcementType.WARRANT, status);
        when(currentEnforcementService.findCurrentEnforcement(CASE_REFERENCE)).thenReturn(Optional.of(warrant));
        return warrant;
    }

    private BailiffAllocationEntity givenActiveAllocation(EnforcementOrderEntity warrant) {
        BailiffAllocationEntity allocation = BailiffAllocationEntity.builder()
            .enforcementOrder(warrant)
            .bailiffUserId("bailiff-user-id")
            .startedAt(NOW.minusSeconds(7200))
            .build();
        when(bailiffAllocationRepository.findByEnforcementOrderIdAndEndedAtIsNull(warrant.getId()))
            .thenReturn(List.of(allocation));
        return allocation;
    }

    private WarrantReturnEntity savedReturn() {
        ArgumentCaptor<WarrantReturnEntity> captor = ArgumentCaptor.forClass(WarrantReturnEntity.class);
        verify(warrantReturnRepository).save(captor.capture());
        return captor.getValue();
    }

    private static EnforcementOrderEntity enforcement(SelectEnforcementType type, EnforcementStatus status) {
        EnforcementOrderEntity enforcement = new EnforcementOrderEntity();
        enforcement.setId(UUID.randomUUID());
        enforcement.setStatus(status);
        enforcement.setStatusUpdatedAt(NOW.minusSeconds(3600));
        enforcement.setEnforcementOrder(EnforcementOrder.builder()
            .chooseEnforcementType(DynamicStringList.builder()
                .value(DynamicStringListElement.builder().code(type.name()).label(type.getLabel()).build())
                .build())
            .build());
        return enforcement;
    }

    private static WarrantReturnDetails interim(InterimWarrantReturnName returnName, String additionalDetails) {
        return WarrantReturnDetails.builder()
            .type(WarrantReturnType.INTERIM)
            .interimReturnName(returnName)
            .additionalDetails(additionalDetails)
            .build();
    }
}
