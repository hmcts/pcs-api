package uk.gov.hmcts.reform.pcs.enforcement.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.enforcement.domain.BailiffNote;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.document.EnforcementDocumentScheduler;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementSummary;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.notification.EnforcementNotificationService;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.enforcement.service.strategy.EnforcementTypeStrategy;
import uk.gov.hmcts.reform.pcs.enforcement.service.strategy.EnforcementTypeStrategyFactory;
import uk.gov.hmcts.reform.pcs.exception.ClaimNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.exception.FeeNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType.WARRANT;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType.WARRANT_OF_RESTITUTION;
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
    private CurrentEnforcementService currentEnforcementService;
    @Mock
    private EnforcementTypeStrategyFactory strategyFactory;
    @Mock
    private EnforcementDocumentScheduler enforcementDocumentScheduler;
    @Mock
    private RepeatWarrantFeeWaiver repeatWarrantFeeWaiver;
    @Mock
    private EnforcementNotificationService enforcementNotificationService;
    @Mock
    private EnforcementFeeService enforcementFeeService;
    @Spy
    private EnforcementStatusTransitions enforcementStatusTransitions =
        new EnforcementStatusTransitions(Clock.fixed(NOW, ZoneOffset.UTC));
    @InjectMocks
    private EnforcementOrderService enforcementOrderService;

    private final UUID claimId = UUID.fromString("a1b2c3d4-e5f6-7890-1234-567890abcdef");
    private final UUID pcsCaseId = UUID.fromString("7b9e0f1a-2b3c-4d5e-6f7a-8b9c0d1e2f3a");

    private static final long CASE_REFERENCE = 1234L;
    private static final Instant NOW = Instant.parse("2026-10-16T10:15:30Z");
    private static final FeeDetails FEE = FeeDetails.builder().code("FEE0380").feeAmount(new BigDecimal("148")).build();

    @BeforeEach
    void stubSaveToReturnTheEnforcementItWasGiven() {
        lenient().when(enforcementOrderRepository.saveAndFlush(any(EnforcementOrderEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @BeforeEach
    void stubTheFeeLookUp() {
        lenient().when(enforcementFeeService.lookUpFee(any())).thenReturn(FEE);
    }

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
    void shouldFindAnApplicationStoredBeforeTheDynamicListReplacedTheType() {
        // Given
        final PcsCaseEntity pcsCaseEntity = buildPcsCaseEntity(pcsCaseId, claimId);
        EnforcementOrderEntity legacyEntity = new EnforcementOrderEntity();
        legacyEntity.setEnforcementOrder(EnforcementOrder.builder().selectEnforcementType(WARRANT).build());
        when(enforcementOrderRepository.findByClaimId(claimId)).thenReturn(List.of(legacyEntity));
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);

        // When
        EnforcementOrder retrieved = enforcementOrderService.retrieveEnforcementOrder(CASE_REFERENCE, WARRANT);

        // Then
        assertThat(retrieved).isSameAs(legacyEntity.getEnforcementOrder());
    }

    @Test
    void shouldIgnoreAStoredApplicationThatRecordsNoEnforcementType() {
        // Given
        final PcsCaseEntity pcsCaseEntity = buildPcsCaseEntity(pcsCaseId, claimId);
        EnforcementOrderEntity noType = new EnforcementOrderEntity();
        noType.setEnforcementOrder(EnforcementOrder.builder().build());
        EnforcementOrderEntity noSelectedValue = new EnforcementOrderEntity();
        noSelectedValue.setEnforcementOrder(
            EnforcementOrder.builder().chooseEnforcementType(DynamicStringList.builder().build()).build());
        when(enforcementOrderRepository.findByClaimId(claimId)).thenReturn(List.of(noType, noSelectedValue));
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);

        // When
        EnforcementOrder retrieved = enforcementOrderService.retrieveEnforcementOrder(CASE_REFERENCE, WARRANT);

        // Then
        assertThat(retrieved).isNull();
    }

    @Test
    void shouldReportAMissingClaimRatherThanFailingOnAnEmptyList() {
        // Given
        final PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setCaseReference(CASE_REFERENCE);
        pcsCaseEntity.setClaims(List.of());

        // When // Then
        assertThatThrownBy(() -> enforcementOrderService.retrieveClaimEntity(pcsCaseEntity))
            .isInstanceOf(ClaimNotFoundException.class)
            .hasMessage("No claim found for case reference " + CASE_REFERENCE);
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
    void shouldPersistTheEnforcementOrderBeforeTheStrategyRuns(SelectEnforcementType enforcementType) {
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
        InOrder inOrder = inOrder(enforcementOrderRepository, strategy);
        inOrder.verify(enforcementOrderRepository).saveAndFlush(any(EnforcementOrderEntity.class));
        inOrder.verify(strategy).process(processedEntityCaptor.capture(), eq(enforcementOrder));

        EnforcementOrderEntity savedEntity = processedEntityCaptor.getValue();
        assertThat(savedEntity.getClaim()).isSameAs(claimEntity);
        assertThat(savedEntity.getEnforcementOrder()).isEqualTo(enforcementOrder);
        assertThat(savedEntity.getStatus()).isEqualTo(EnforcementStatus.AWAITING_PAYMENT);
        assertThat(savedEntity.getStatusUpdatedAt()).isEqualTo(NOW);
        assertThat(savedEntity.getSubmittedAt()).isEqualTo(NOW);
        verify(draftCaseDataService).deleteUnsubmittedCaseData(anyLong(), any(EventId.class));
        verifyNoInteractions(enforcementDocumentScheduler);
        // The application is not received until its fee is paid.
        verifyNoInteractions(enforcementNotificationService);
    }

    @Test
    void shouldResolveTheTypeOfAnApplicationCarryingOnlyTheOldEnforcementTypeField() {
        // Given
        final PcsCaseEntity pcsCaseEntity = buildPcsCaseEntity(pcsCaseId, claimId);
        final EnforcementOrder enforcementOrder = EnforcementOrder.builder().selectEnforcementType(WRIT).build();
        final EnforcementTypeStrategy strategy = mock(EnforcementTypeStrategy.class);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(strategyFactory.getStrategy(WRIT)).thenReturn(strategy);

        // When
        enforcementOrderService.saveAndClearDraftData(CASE_REFERENCE, enforcementOrder);

        // Then
        verify(strategy).process(any(EnforcementOrderEntity.class), eq(enforcementOrder));
    }

    @ParameterizedTest
    @EnumSource(value = SelectEnforcementType.class, names = {"WARRANT_OF_RESTITUTION", "WRIT_OF_RESTITUTION"})
    void shouldStartAnApplicationWithNoFeeAwaitingReview(SelectEnforcementType enforcementType) {
        // Given
        final PcsCaseEntity pcsCaseEntity = buildPcsCaseEntity(pcsCaseId, claimId);
        final EnforcementOrder enforcementOrder = buildEnforcementOrderWithSpecifiedType(enforcementType);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(strategyFactory.getStrategy(enforcementType)).thenReturn(mock(EnforcementTypeStrategy.class));

        // When
        enforcementOrderService.saveAndClearDraftData(CASE_REFERENCE, enforcementOrder);

        // Then
        ArgumentCaptor<EnforcementOrderEntity> savedCaptor = ArgumentCaptor.forClass(EnforcementOrderEntity.class);
        verify(enforcementOrderRepository).saveAndFlush(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getStatus()).isEqualTo(EnforcementStatus.AWAITING_REVIEW);
        assertThat(savedCaptor.getValue().getStatusUpdatedAt()).isEqualTo(NOW);
        verify(enforcementDocumentScheduler)
            .scheduleApplicationDocuments(savedCaptor.getValue().getId(), enforcementType);
        verify(enforcementNotificationService).notifyClaimantOfApplicationReceived(CASE_REFERENCE, enforcementType);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldStartAWarrantAwaitingReviewWhenItsFeeIsWaived(boolean waived) {
        // Given
        final PcsCaseEntity pcsCaseEntity = buildPcsCaseEntity(pcsCaseId, claimId);
        final EnforcementOrder enforcementOrder = buildEnforcementOrderWithSpecifiedType(WARRANT);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(strategyFactory.getStrategy(WARRANT)).thenReturn(mock(EnforcementTypeStrategy.class));
        when(repeatWarrantFeeWaiver.isFeeWaived(claimId, WARRANT)).thenReturn(waived);

        // When
        EnforcementChange<FeeDetails> submitted =
            enforcementOrderService.saveAndClearDraftData(CASE_REFERENCE, enforcementOrder);

        // Then
        ArgumentCaptor<EnforcementOrderEntity> savedCaptor = ArgumentCaptor.forClass(EnforcementOrderEntity.class);
        verify(enforcementOrderRepository).saveAndFlush(savedCaptor.capture());
        assertThat(submitted.change() != null).isEqualTo(!waived);
        assertThat(savedCaptor.getValue().getStatus())
            .isEqualTo(waived ? EnforcementStatus.AWAITING_REVIEW : EnforcementStatus.AWAITING_PAYMENT);
        if (waived) {
            verify(enforcementNotificationService).notifyClaimantOfApplicationReceived(CASE_REFERENCE, WARRANT);
            verify(enforcementDocumentScheduler).scheduleApplicationDocuments(savedCaptor.getValue().getId(), WARRANT);
        } else {
            verifyNoInteractions(enforcementNotificationService, enforcementDocumentScheduler);
        }
    }

    @ParameterizedTest
    @EnumSource(value = SelectEnforcementType.class, names = {"WARRANT_OF_RESTITUTION", "WRIT_OF_RESTITUTION"})
    void shouldReportThatAnApplicationWithNoFeeOwesNothing(SelectEnforcementType enforcementType) {
        // Given
        final PcsCaseEntity pcsCaseEntity = buildPcsCaseEntity(pcsCaseId, claimId);
        final EnforcementOrder enforcementOrder = buildEnforcementOrderWithSpecifiedType(enforcementType);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(strategyFactory.getStrategy(enforcementType)).thenReturn(mock(EnforcementTypeStrategy.class));

        // When
        EnforcementChange<FeeDetails> submitted =
            enforcementOrderService.saveAndClearDraftData(CASE_REFERENCE, enforcementOrder);

        // Then
        assertThat(submitted.change()).isNull();
        verifyNoInteractions(repeatWarrantFeeWaiver);
    }

    /**
     * The claimant is asked to submit again rather than given an unexplained 500, and the application is
     * rolled back with the documents it could not queue, draft and all.
     */
    @Test
    void shouldRefuseAFreeApplicationWhoseDocumentsCannotBeQueued() {
        final EnforcementOrder enforcementOrder = buildEnforcementOrderWithSpecifiedType(WARRANT_OF_RESTITUTION);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(buildPcsCaseEntity(pcsCaseId, claimId));
        when(strategyFactory.getStrategy(WARRANT_OF_RESTITUTION)).thenReturn(mock(EnforcementTypeStrategy.class));
        doThrow(new DataIntegrityViolationException("scheduled_tasks"))
            .when(enforcementDocumentScheduler).scheduleApplicationDocuments(any(), any());

        EnforcementChange<FeeDetails> submitted =
            enforcementOrderService.saveAndClearDraftData(CASE_REFERENCE, enforcementOrder);

        assertThat(submitted.errors()).containsExactly(EnforcementSideEffects.COULD_NOT_SCHEDULE_ERROR);
        verifyNoInteractions(draftCaseDataService);
    }

    @Test
    void shouldRefuseAPayableApplicationWhoseServiceRequestCannotBeQueued() {
        final EnforcementOrder enforcementOrder = buildEnforcementOrderWithSpecifiedType(WARRANT);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(buildPcsCaseEntity(pcsCaseId, claimId));
        when(strategyFactory.getStrategy(WARRANT)).thenReturn(mock(EnforcementTypeStrategy.class));
        doThrow(new DataIntegrityViolationException("scheduled_tasks"))
            .when(enforcementFeeService).requestPayment(any(), any(), any());

        EnforcementChange<FeeDetails> submitted =
            enforcementOrderService.saveAndClearDraftData(CASE_REFERENCE, enforcementOrder);

        assertThat(submitted.errors()).containsExactly(EnforcementSideEffects.COULD_NOT_SCHEDULE_ERROR);
        verifyNoInteractions(draftCaseDataService);
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
        enforcementOrder.setBailiffNotes(List.of(new ListValue<>("1", new BailiffNote())));
        enforcementOrder.setBailiffNoteText("note");
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(strategyFactory.getStrategy(WRIT)).thenReturn(mock(EnforcementTypeStrategy.class));

        // When
        enforcementOrderService.saveAndClearDraftData(CASE_REFERENCE, enforcementOrder);

        // Then
        ArgumentCaptor<EnforcementOrderEntity> savedCaptor = ArgumentCaptor.forClass(EnforcementOrderEntity.class);
        verify(enforcementOrderRepository).saveAndFlush(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getEnforcementOrder().getEnforcementStatus()).isNull();
        assertThat(savedCaptor.getValue().getEnforcementOrder().getBailiffNotes()).isNull();
        assertThat(savedCaptor.getValue().getEnforcementOrder().getBailiffNoteText()).isNull();
        assertThat(savedCaptor.getValue().getStatus()).isEqualTo(EnforcementStatus.AWAITING_PAYMENT);
    }

    @Test
    void shouldNotStoreTheViewEnforcementListInTheEnforcementOrderJson() {
        // Given
        final PcsCaseEntity pcsCaseEntity = buildPcsCaseEntity(pcsCaseId, claimId);
        final EnforcementOrder enforcementOrder = buildEnforcementOrderWithSpecifiedType(WARRANT);
        enforcementOrder.setEnforcements(List.of(ListValue.<EnforcementSummary>builder()
                                                     .id("earlier-warrant")
                                                     .value(EnforcementSummary.builder()
                                                                .enforcementType(WARRANT)
                                                                .status(EnforcementStatus.CLOSED)
                                                                .build())
                                                     .build()));
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(strategyFactory.getStrategy(WARRANT)).thenReturn(mock(EnforcementTypeStrategy.class));

        // When
        enforcementOrderService.saveAndClearDraftData(CASE_REFERENCE, enforcementOrder);

        // Then
        ArgumentCaptor<EnforcementOrderEntity> savedCaptor = ArgumentCaptor.forClass(EnforcementOrderEntity.class);
        verify(enforcementOrderRepository).saveAndFlush(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getEnforcementOrder().getEnforcements()).isNull();
    }

    @ParameterizedTest
    @EnumSource(EnforcementStatus.class)
    void shouldReportWhetherTheClaimHasALiveEnforcement(EnforcementStatus status) {
        // Given
        when(currentEnforcementService.findCurrentEnforcement(CASE_REFERENCE))
            .thenReturn(Optional.of(EnforcementOrderEntity.builder().status(status).build()));

        // When
        boolean result = enforcementOrderService.hasLiveEnforcement(CASE_REFERENCE);

        // Then
        assertThat(result).isEqualTo(status.isLive());
    }

    @Test
    void shouldReportNoLiveEnforcementWhenTheClaimHasNone() {
        when(currentEnforcementService.findCurrentEnforcement(CASE_REFERENCE)).thenReturn(Optional.empty());

        assertThat(enforcementOrderService.hasLiveEnforcement(CASE_REFERENCE)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(EnforcementStatus.class)
    void shouldOnlyLetTheClaimantApplyWhenTheCurrentEnforcementIsNotLive(EnforcementStatus status) {
        assertThat(EnforcementOrderService.canApplyForEnforcement(
            EnforcementOrderEntity.builder().status(status).build())).isEqualTo(!status.isLive());
    }

    @Test
    void shouldLetTheClaimantApplyWhenTheClaimHasNoEnforcement() {
        assertThat(EnforcementOrderService.canApplyForEnforcement(null)).isTrue();
    }

    private static Stream<Arguments> enforcementOrderTypes() {
        return Stream.of(
                arguments(WARRANT),
                arguments(WRIT)
        );
    }

    @ParameterizedTest
    @MethodSource("enforcementOrderTypes")
    void shouldRaiseAServiceRequestForTheFeeOwed(SelectEnforcementType enforcementType) {
        // Given
        final PcsCaseEntity pcsCaseEntity = buildPcsCaseEntity(pcsCaseId, claimId);
        final EnforcementOrder enforcementOrder = buildEnforcementOrderWithSpecifiedType(enforcementType);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(strategyFactory.getStrategy(enforcementType)).thenReturn(mock(EnforcementTypeStrategy.class));

        // When
        EnforcementChange<FeeDetails> submitted =
            enforcementOrderService.saveAndClearDraftData(CASE_REFERENCE, enforcementOrder);

        // Then
        ArgumentCaptor<EnforcementOrderEntity> savedCaptor = ArgumentCaptor.forClass(EnforcementOrderEntity.class);
        verify(enforcementOrderRepository).saveAndFlush(savedCaptor.capture());
        assertThat(submitted.change()).isEqualTo(FEE);
        verify(enforcementFeeService).lookUpFee(enforcementType);
        verify(enforcementFeeService).requestPayment(pcsCaseEntity, savedCaptor.getValue().getId(), FEE);
    }

    @Test
    void shouldRaiseNoServiceRequestForAFreeRepeatWarrant() {
        // Given
        final PcsCaseEntity pcsCaseEntity = buildPcsCaseEntity(pcsCaseId, claimId);
        final EnforcementOrder enforcementOrder = buildEnforcementOrderWithSpecifiedType(WARRANT);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(strategyFactory.getStrategy(WARRANT)).thenReturn(mock(EnforcementTypeStrategy.class));
        when(repeatWarrantFeeWaiver.isFeeWaived(claimId, WARRANT)).thenReturn(true);

        // When
        enforcementOrderService.saveAndClearDraftData(CASE_REFERENCE, enforcementOrder);

        // Then
        verifyNoInteractions(enforcementFeeService);
    }

    @ParameterizedTest
    @EnumSource(value = SelectEnforcementType.class, names = {"WARRANT_OF_RESTITUTION", "WRIT_OF_RESTITUTION"})
    void shouldRaiseNoServiceRequestForATypeWithNoFee(SelectEnforcementType enforcementType) {
        // Given
        final PcsCaseEntity pcsCaseEntity = buildPcsCaseEntity(pcsCaseId, claimId);
        final EnforcementOrder enforcementOrder = buildEnforcementOrderWithSpecifiedType(enforcementType);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(strategyFactory.getStrategy(enforcementType)).thenReturn(mock(EnforcementTypeStrategy.class));

        // When
        enforcementOrderService.saveAndClearDraftData(CASE_REFERENCE, enforcementOrder);

        // Then
        verifyNoInteractions(enforcementFeeService);
    }

    @Test
    void shouldSaveNothingWhenTheFeeOwedCannotBeLookedUp() {
        // Given
        final PcsCaseEntity pcsCaseEntity = buildPcsCaseEntity(pcsCaseId, claimId);
        final EnforcementOrder enforcementOrder = buildEnforcementOrderWithSpecifiedType(WRIT);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(enforcementFeeService.lookUpFee(WRIT)).thenThrow(new FeeNotFoundException("Unable to retrieve fee"));

        // When
        assertThatThrownBy(() -> enforcementOrderService.saveAndClearDraftData(CASE_REFERENCE, enforcementOrder))
            .isInstanceOf(FeeNotFoundException.class);

        // Then
        verify(enforcementOrderRepository, never()).saveAndFlush(any());
        verify(enforcementFeeService, never()).requestPayment(any(), any(), any());
        verify(draftCaseDataService, never()).deleteUnsubmittedCaseData(anyLong(), any(EventId.class));
    }

    @ParameterizedTest
    @MethodSource("enforcementOrderTypes")
    void shouldMoveAPaidApplicationToAwaitingReview(SelectEnforcementType type) {
        EnforcementOrderEntity awaitingPayment = new EnforcementOrderEntity();
        awaitingPayment.setId(UUID.randomUUID());
        awaitingPayment.setEnforcementOrder(buildEnforcementOrderWithSpecifiedType(type));
        awaitingPayment.setStatus(EnforcementStatus.AWAITING_PAYMENT);

        enforcementOrderService.moveToAwaitingReview(CASE_REFERENCE, awaitingPayment);

        assertThat(awaitingPayment.getStatus()).isEqualTo(EnforcementStatus.AWAITING_REVIEW);
        assertThat(awaitingPayment.getStatusUpdatedAt()).isEqualTo(NOW);
        verify(enforcementOrderRepository).save(awaitingPayment);
        verify(enforcementDocumentScheduler).scheduleApplicationDocuments(awaitingPayment.getId(), type);
        verify(enforcementNotificationService).notifyClaimantOfApplicationReceived(CASE_REFERENCE, type);
    }
}
