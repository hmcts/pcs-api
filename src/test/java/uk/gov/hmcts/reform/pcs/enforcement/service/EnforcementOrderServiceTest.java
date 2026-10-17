package uk.gov.hmcts.reform.pcs.enforcement.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.enforcement.service.strategy.EnforcementTypeStrategy;
import uk.gov.hmcts.reform.pcs.enforcement.service.strategy.EnforcementTypeStrategyFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType.WARRANT;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType.WRIT;
import static uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementDataUtil.buildEnforcementOrderWithSpecifiedType;
import static uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementDataUtil.buildPcsCaseEntity;

@ExtendWith(MockitoExtension.class)
class EnforcementOrderServiceTest {

    @Mock
    private DraftCaseDataService draftCaseDataService;
    @Mock
    private EnforcementOrderRepository enforcementOrderRepository;
    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private EnforcementTypeStrategyFactory strategyFactory;
    @Spy
    private EnforcementStatusTransitions enforcementStatusTransitions =
        new EnforcementStatusTransitions(Clock.fixed(NOW, ZoneOffset.UTC));
    @InjectMocks
    private EnforcementOrderService enforcementOrderService;

    private final UUID claimId = UUID.fromString("a1b2c3d4-e5f6-7890-1234-567890abcdef");
    private final UUID pcsCaseId = UUID.fromString("7b9e0f1a-2b3c-4d5e-6f7a-8b9c0d1e2f3a");

    private static final long CASE_REFERENCE = 1234L;
    private static final Instant NOW = Instant.parse("2026-10-16T10:15:30Z");

    @Test
    void shouldReturnEnforcementOrderIfFoundInDatabase() {
        // Given
        final PcsCaseEntity pcsCaseEntity = buildPcsCaseEntity(pcsCaseId, claimId);
        ClaimEntity claimEntity = new ClaimEntity();
        claimEntity.setId(claimId);
        pcsCaseEntity.setClaims(List.of(claimEntity));
        EnforcementOrderEntity enforcementOrderEntity = new EnforcementOrderEntity();
        enforcementOrderEntity.setClaim(claimEntity);
        EnforcementOrder enforcementOrder = EnforcementDataUtil.buildEnforcementOrder();
        enforcementOrderEntity.setEnforcementOrder(enforcementOrder);
        when(enforcementOrderRepository.findByClaimId(claimId)).thenReturn(List.of(enforcementOrderEntity));
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        // When
        EnforcementOrder retOrder = enforcementOrderService.retrieveEnforcementOrder(CASE_REFERENCE, WARRANT);

        // Then
        assertThat(retOrder).isNotNull();
    }

    @Test
    void shouldReturnNullIfNotFoundInDatabase() {
        // Given
        final PcsCaseEntity pcsCaseEntity = buildPcsCaseEntity(pcsCaseId, claimId);
        ClaimEntity claimEntity = new ClaimEntity();
        pcsCaseEntity.setClaims(List.of(claimEntity));
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);

        // When
        EnforcementOrder retOrder = enforcementOrderService.retrieveEnforcementOrder(CASE_REFERENCE, WARRANT);

        // Then
        assertThat(retOrder).isNull();
    }

    @Test
    void shouldReturnFirstClaimFromPcsCaseEntity() {
        // Given
        final PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        ClaimEntity firstClaim = new ClaimEntity();
        ClaimEntity secondClaim = new ClaimEntity();
        pcsCaseEntity.setClaims(List.of(firstClaim, secondClaim));

        // When
        ClaimEntity result = enforcementOrderService.retrieveClaimEntity(pcsCaseEntity);

        // Then
        assertThat(result).isSameAs(firstClaim);
    }

    @ParameterizedTest
    @MethodSource("enforcementOrderTypes")
    void shouldSetDependenciesDataBeforeSavingEnforcementOrderData(SelectEnforcementType enforcementType) {
        // Given
        final PcsCaseEntity pcsCaseEntity = buildPcsCaseEntity(pcsCaseId, claimId);
        final ClaimEntity claimEntity = new ClaimEntity();
        pcsCaseEntity.setClaims(List.of(claimEntity));
        final EnforcementOrder enforcementOrder = buildEnforcementOrderWithSpecifiedType(enforcementType);
        final EnforcementTypeStrategy strategy = mock(EnforcementTypeStrategy.class);

        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(strategyFactory.getStrategy(enforcementType)).thenReturn(strategy);

        // When
        enforcementOrderService.saveAndClearDraftData(CASE_REFERENCE, enforcementOrder);

        // Then
        ArgumentCaptor<EnforcementOrderEntity> processedEntityCaptor =
                ArgumentCaptor.forClass(EnforcementOrderEntity.class);
        inOrder(strategyFactory, strategy, enforcementOrderRepository)
                .verify(strategy).process(processedEntityCaptor.capture(), eq(enforcementOrder));
        inOrder(strategyFactory, strategy, enforcementOrderRepository)
                .verify(enforcementOrderRepository).save(processedEntityCaptor.getValue());

        EnforcementOrderEntity savedEntity = processedEntityCaptor.getValue();
        assertThat(savedEntity.getClaim()).isSameAs(claimEntity);
        assertThat(savedEntity.getEnforcementOrder()).isEqualTo(enforcementOrder);
        assertThat(savedEntity.getStatus()).isEqualTo(EnforcementStatus.AWAITING_PAYMENT);
        assertThat(savedEntity.getStatusUpdatedAt()).isEqualTo(NOW);
        verify(draftCaseDataService).deleteUnsubmittedCaseData(anyLong(), any(EventId.class));
    }

    @Test
    void shouldSetTheInitialStatusBeforeTheStrategyRuns() {
        // Given
        final PcsCaseEntity pcsCaseEntity = buildPcsCaseEntity(pcsCaseId, claimId);
        final EnforcementOrder enforcementOrder = buildEnforcementOrderWithSpecifiedType(WARRANT);
        final EnforcementTypeStrategy strategy = mock(EnforcementTypeStrategy.class);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(strategyFactory.getStrategy(WARRANT)).thenReturn(strategy);
        List<EnforcementStatus> statusSeenByStrategy = new ArrayList<>();
        doAnswer(invocation -> {
            statusSeenByStrategy.add(invocation.<EnforcementOrderEntity>getArgument(0).getStatus());
            return null;
        }).when(strategy).process(any(), any());

        // When
        enforcementOrderService.saveAndClearDraftData(CASE_REFERENCE, enforcementOrder);

        // Then
        assertThat(statusSeenByStrategy).containsExactly(EnforcementStatus.AWAITING_PAYMENT);
    }

    @Test
    void shouldNotStoreTheViewStatusInTheEnforcementOrderJson() {
        // Given
        final PcsCaseEntity pcsCaseEntity = buildPcsCaseEntity(pcsCaseId, claimId);
        final EnforcementOrder enforcementOrder = buildEnforcementOrderWithSpecifiedType(WRIT);
        enforcementOrder.setEnforcementStatus(EnforcementStatus.CLOSED);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(strategyFactory.getStrategy(WRIT)).thenReturn(mock(EnforcementTypeStrategy.class));

        // When
        enforcementOrderService.saveAndClearDraftData(CASE_REFERENCE, enforcementOrder);

        // Then
        ArgumentCaptor<EnforcementOrderEntity> savedCaptor = ArgumentCaptor.forClass(EnforcementOrderEntity.class);
        verify(enforcementOrderRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getEnforcementOrder().getEnforcementStatus()).isNull();
        assertThat(savedCaptor.getValue().getStatus()).isEqualTo(EnforcementStatus.AWAITING_PAYMENT);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldReportWhetherTheClaimHasALiveEnforcement(boolean live) {
        // Given
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(buildPcsCaseEntity(pcsCaseId, claimId));
        when(enforcementOrderRepository.existsByClaimIdAndStatusNotIn(claimId, EnforcementStatus.notLive()))
            .thenReturn(live);

        // When
        boolean result = enforcementOrderService.hasLiveEnforcement(CASE_REFERENCE);

        // Then
        assertThat(result).isEqualTo(live);
    }

    private static Stream<Arguments> enforcementOrderTypes() {
        return Stream.of(
                arguments(WARRANT),
                arguments(WRIT)
        );
    }

    @Test
    void shouldMoveTheApplicationAwaitingPaymentToAwaitingReviewWhenTheFeeIsPaid() {
        final PcsCaseEntity pcsCaseEntity = buildPcsCaseEntity(pcsCaseId, claimId);
        EnforcementOrderEntity awaitingPayment = new EnforcementOrderEntity();
        awaitingPayment.setStatus(EnforcementStatus.AWAITING_PAYMENT);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(enforcementOrderRepository.findByClaimIdAndStatus(claimId, EnforcementStatus.AWAITING_PAYMENT))
            .thenReturn(List.of(awaitingPayment));

        boolean recorded = enforcementOrderService.recordFeePaid(CASE_REFERENCE);

        assertThat(recorded).isTrue();
        assertThat(awaitingPayment.getStatus()).isEqualTo(EnforcementStatus.AWAITING_REVIEW);
        assertThat(awaitingPayment.getStatusUpdatedAt()).isEqualTo(NOW);
        verify(enforcementOrderRepository).save(awaitingPayment);
    }

    @Test
    void shouldRecordNoFeeWhenNoApplicationIsAwaitingPayment() {
        final PcsCaseEntity pcsCaseEntity = buildPcsCaseEntity(pcsCaseId, claimId);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(enforcementOrderRepository.findByClaimIdAndStatus(claimId, EnforcementStatus.AWAITING_PAYMENT))
            .thenReturn(List.of());

        assertThat(enforcementOrderService.recordFeePaid(CASE_REFERENCE)).isFalse();
        verify(enforcementOrderRepository, org.mockito.Mockito.never()).save(any());
    }
}
