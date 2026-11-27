package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementState;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.strategy.EnforcementTypeStrategy;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.strategy.EnforcementTypeStrategyFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementState.DRAFT_DISCARDED;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType.WARRANT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType.WRIT;
import static uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.EnforcementDataUtil.buildEnforcementOrderWithSpecifiedType;
import static uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.EnforcementDataUtil.buildPcsCaseEntity;

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
    @InjectMocks
    private EnforcementOrderService enforcementOrderService;

    private final UUID claimId = UUID.fromString("a1b2c3d4-e5f6-7890-1234-567890abcdef");
    private final UUID pcsCaseId = UUID.fromString("7b9e0f1a-2b3c-4d5e-6f7a-8b9c0d1e2f3a");

    private static final long CASE_REFERENCE = 1234L;

    @Test
    void shouldReturnEnforcementOrderIfFoundInDatabase() {
        // Given
        final PcsCaseEntity pcsCaseEntity = buildPcsCaseEntity(pcsCaseId, claimId);
        EnforcementOrderEntity enforcementOrderEntity = new EnforcementOrderEntity();
        EnforcementOrder enforcementOrder = EnforcementDataUtil.buildEnforcementOrder();
        enforcementOrderEntity.setEnforcementOrder(enforcementOrder);
        pcsCaseEntity.setEnforcementOrders(Set.of(enforcementOrderEntity));
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
    void shouldNotReturnDraftDiscardedEnforcementOrder() {
        // Given
        final PcsCaseEntity pcsCaseEntity = buildPcsCaseEntity(pcsCaseId, claimId);
        final EnforcementOrderEntity enforcementOrderEntity = EnforcementOrderEntity.builder()
                .enforcementOrder(buildEnforcementOrderWithSpecifiedType(WARRANT))
                .state(DRAFT_DISCARDED)
                .build();
        pcsCaseEntity.setEnforcementOrders(Set.of(enforcementOrderEntity));
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);

        // When
        EnforcementOrder retOrder = enforcementOrderService.retrieveEnforcementOrder(CASE_REFERENCE, WARRANT);

        // Then
        assertThat(retOrder).isNull();
    }

    @Test
    void shouldMarkAwaitingEnforcementOrderAsDraftDiscarded() {
        // Given
        final PcsCaseEntity pcsCaseEntity = buildPcsCaseEntity(pcsCaseId, claimId);
        final EnforcementOrderEntity enforcementOrderEntity = EnforcementOrderEntity.builder()
                .pcsCase(pcsCaseEntity)
                .state(EnforcementState.AWAITING_SUBMISSION_TO_HMCTS)
                .build();
        pcsCaseEntity.setEnforcementOrders(Set.of(enforcementOrderEntity));
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);

        // When
        enforcementOrderService.markEnforcementApplicationAsDraftDiscarded(CASE_REFERENCE);

        // Then
        assertThat(enforcementOrderEntity.getState()).isEqualTo(DRAFT_DISCARDED);
        verify(enforcementOrderRepository).save(enforcementOrderEntity);
    }

    @Test
    void shouldReuseDraftDiscardedEnforcementOrderWhenCreatingEnforcementOrder() {
        // Given
        final PcsCaseEntity pcsCaseEntity = buildPcsCaseEntity(pcsCaseId, claimId);
        final EnforcementOrderEntity enforcementOrderEntity = EnforcementOrderEntity.builder()
                .pcsCase(pcsCaseEntity)
                .enforcementOrder(buildEnforcementOrderWithSpecifiedType(WARRANT))
                .state(DRAFT_DISCARDED)
                .build();
        pcsCaseEntity.setEnforcementOrders(new HashSet<>(Set.of(enforcementOrderEntity)));
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);

        // When
        enforcementOrderService.createEnforcementOrder(CASE_REFERENCE);

        // Then
        assertThat(pcsCaseEntity.getEnforcementOrders()).containsExactly(enforcementOrderEntity);
        assertThat(enforcementOrderEntity.getState()).isEqualTo(EnforcementState.AWAITING_SUBMISSION_TO_HMCTS);
        verify(enforcementOrderRepository).save(enforcementOrderEntity);
    }

    @Test
    void shouldNotCreateAnotherEnforcementOrderWhenOneIsAwaitingSubmission() {
        // Given
        final PcsCaseEntity pcsCaseEntity = buildPcsCaseEntity(pcsCaseId, claimId);
        final EnforcementOrderEntity enforcementOrderEntity = EnforcementOrderEntity.builder()
                .pcsCase(pcsCaseEntity)
                .state(EnforcementState.AWAITING_SUBMISSION_TO_HMCTS)
                .build();
        pcsCaseEntity.setEnforcementOrders(new HashSet<>(Set.of(enforcementOrderEntity)));
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);

        // When
        enforcementOrderService.createEnforcementOrder(CASE_REFERENCE);

        // Then
        assertThat(pcsCaseEntity.getEnforcementOrders()).containsExactly(enforcementOrderEntity);
        verify(enforcementOrderRepository, never()).save(any(EnforcementOrderEntity.class));
    }

    @ParameterizedTest
    @MethodSource("enforcementOrderTypes")
    void shouldCreateAndSubmitEnforcementOrder(SelectEnforcementType enforcementType) {
        // Given
        final PcsCaseEntity pcsCaseEntity = buildPcsCaseEntity(pcsCaseId, claimId);
        final EnforcementOrder enforcementOrder = buildEnforcementOrderWithSpecifiedType(enforcementType);
        final EnforcementTypeStrategy strategy = mock(EnforcementTypeStrategy.class);

        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(strategyFactory.getStrategy(enforcementType)).thenReturn(strategy);
        when(enforcementOrderRepository.save(any(EnforcementOrderEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        enforcementOrderService.retrieveEnforcementOrder(CASE_REFERENCE, WARRANT);
        enforcementOrderService.createEnforcementOrder(CASE_REFERENCE);
        EnforcementOrderEntity enforcementOrderEntity = pcsCaseEntity.getEnforcementOrders().iterator().next();

        // When
        EnforcementOrderEntity result =
                enforcementOrderService.submitEnforcementOrder(CASE_REFERENCE, enforcementOrder);

        // Then
        ArgumentCaptor<EnforcementOrderEntity> processedEntityCaptor =
                ArgumentCaptor.forClass(EnforcementOrderEntity.class);
        var saveOrder = inOrder(strategyFactory, strategy, draftCaseDataService, enforcementOrderRepository);
        saveOrder.verify(strategyFactory).getStrategy(enforcementType);
        saveOrder.verify(strategy).process(processedEntityCaptor.capture(), eq(enforcementOrder));
        saveOrder.verify(draftCaseDataService).deleteUnsubmittedCaseData(CASE_REFERENCE, EventId.enforceTheOrder);
        saveOrder.verify(enforcementOrderRepository).save(processedEntityCaptor.getValue());

        EnforcementOrderEntity savedEntity = processedEntityCaptor.getValue();
        assertThat(savedEntity).isSameAs(enforcementOrderEntity);
        assertThat(savedEntity.getEnforcementOrder()).isEqualTo(enforcementOrder);
        assertThat(savedEntity.getState()).isEqualTo(EnforcementState.PENDING_CASE_ISSUED);
        assertThat(result).isSameAs(savedEntity);
    }

    @Test
    void shouldRankTheFirstApplicationOfATypeOne() {
        EnforcementOrderEntity saved = saveWarrantOnCaseWith(List.of());

        assertThat(saved.getRank()).isEqualTo(1);
    }

    @Test
    void shouldRankOneMoreThanTheCasesExistingApplicationsOfTheSameType() {
        EnforcementOrderEntity existingWrit = EnforcementOrderEntity.builder()
            .enforcementOrder(buildEnforcementOrderWithSpecifiedType(WRIT)).build();
        EnforcementOrderEntity existingWarrant = EnforcementOrderEntity.builder()
            .enforcementOrder(buildEnforcementOrderWithSpecifiedType(WARRANT)).build();

        EnforcementOrderEntity saved = saveWarrantOnCaseWith(List.of(existingWrit, existingWarrant));

        assertThat(saved.getRank()).isEqualTo(2);
    }

    @Test
    void shouldNotCountApplicationsWithoutAnEnforcementTypeWhenRanking() {
        EnforcementOrderEntity withoutOrder = EnforcementOrderEntity.builder().build();
        EnforcementOrderEntity withoutType = EnforcementOrderEntity.builder()
            .enforcementOrder(EnforcementOrder.builder().build()).build();

        EnforcementOrderEntity saved = saveWarrantOnCaseWith(List.of(withoutOrder, withoutType));

        assertThat(saved.getRank()).isEqualTo(1);
    }

    private EnforcementOrderEntity saveWarrantOnCaseWith(List<EnforcementOrderEntity> existingApplications) {
        PcsCaseEntity pcsCaseEntity = buildPcsCaseEntity(pcsCaseId, claimId);
        EnforcementOrderEntity draft = EnforcementOrderEntity.builder()
            .state(EnforcementState.AWAITING_SUBMISSION_TO_HMCTS)
            .pcsCase(pcsCaseEntity)
            .build();
        Set<EnforcementOrderEntity> applications = new HashSet<>(existingApplications);
        applications.add(draft);
        pcsCaseEntity.setEnforcementOrders(applications);
        final EnforcementOrder enforcementOrder = buildEnforcementOrderWithSpecifiedType(WARRANT);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(strategyFactory.getStrategy(WARRANT)).thenReturn(mock(EnforcementTypeStrategy.class));
        when(enforcementOrderRepository.save(any(EnforcementOrderEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        return enforcementOrderService.submitEnforcementOrder(CASE_REFERENCE, enforcementOrder);
    }

    private static Stream<Arguments> enforcementOrderTypes() {
        return Stream.of(
                arguments(WARRANT),
                arguments(WRIT)
        );
    }

}
