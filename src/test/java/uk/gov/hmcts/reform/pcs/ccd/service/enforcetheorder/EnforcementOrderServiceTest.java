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
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.strategy.EnforcementTypeStrategy;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.strategy.EnforcementTypeStrategyFactory;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementState.PENDING_CASE_ISSUED;
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
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);

        // When
        EnforcementOrder retOrder = enforcementOrderService.retrieveEnforcementOrder(CASE_REFERENCE, WARRANT);

        // Then
        assertThat(retOrder).isNull();
    }

    @Test
    void shouldChangeLiveEnforcementToBreathingSpace() {
        long caseReference = 1234567890123456L;

        EnforcementOrderEntity application =
            EnforcementOrderEntity.builder()
                .state(EnforcementState.CASE_PROGRESSION)
                .build();

        PcsCaseEntity pcsCase = PcsCaseEntity.builder()
            .enforcementOrders(Set.of(application))
            .build();

        when(pcsCaseService.loadCase(caseReference))
            .thenReturn(pcsCase);

        enforcementOrderService.updateStatesForBreathingSpace(caseReference);

        assertThat(application.getState())
            .isEqualTo(EnforcementState.BREATHING_SPACE);
    }

    @Test
    void shouldChangeDraftEnforcementToDraftState() {
        long caseReference = 1234567890123456L;

        EnforcementOrderEntity application =
            EnforcementOrderEntity.builder()
                .state(EnforcementState.AWAITING_SUBMISSION_TO_HMCTS)
                .build();

        PcsCaseEntity pcsCase = PcsCaseEntity.builder()
            .enforcementOrders(Set.of(application))
            .build();

        when(pcsCaseService.loadCase(caseReference))
            .thenReturn(pcsCase);

        enforcementOrderService.updateStatesForBreathingSpace(caseReference);

        assertThat(application.getState())
            .isEqualTo(EnforcementState.DRAFT_DISCARD);
    }

    @Test
    void shouldChangePendingEnforcementToDraftState() {
        long caseReference = 1234567890123456L;

        EnforcementOrderEntity application =
            EnforcementOrderEntity.builder()
                .state(PENDING_CASE_ISSUED)
                .build();

        PcsCaseEntity pcsCase = PcsCaseEntity.builder()
            .enforcementOrders(Set.of(application))
            .build();

        when(pcsCaseService.loadCase(caseReference))
            .thenReturn(pcsCase);

        enforcementOrderService.updateStatesForBreathingSpace(caseReference);

        assertThat(application.getState())
            .isEqualTo(EnforcementState.DRAFT_DISCARD);
    }

    @Test
    void shouldHandleNoEnforcementApplications() {
        PcsCaseEntity pcsCase = PcsCaseEntity.builder()
            .enforcementOrders(Set.of())
            .build();

        when(pcsCaseService.loadCase(CASE_REFERENCE))
            .thenReturn(pcsCase);

        enforcementOrderService.updateStatesForBreathingSpace(CASE_REFERENCE);

        verify(enforcementOrderRepository, never()).save(any());
    }


    @ParameterizedTest
    @MethodSource("enforcementOrderTypes")
    void shouldSetDependenciesDataBeforeSavingEnforcementOrderData(SelectEnforcementType enforcementType) {
        // Given
        final PcsCaseEntity pcsCaseEntity = buildPcsCaseEntity(pcsCaseId, claimId);
        final EnforcementOrder enforcementOrder = buildEnforcementOrderWithSpecifiedType(enforcementType);
        final EnforcementTypeStrategy strategy = mock(EnforcementTypeStrategy.class);

        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(strategyFactory.getStrategy(enforcementType)).thenReturn(strategy);

        // When
        enforcementOrderService.saveAndClearDraftData(CASE_REFERENCE, enforcementOrder, PENDING_CASE_ISSUED);

        // Then
        ArgumentCaptor<EnforcementOrderEntity> processedEntityCaptor =
                ArgumentCaptor.forClass(EnforcementOrderEntity.class);
        inOrder(strategyFactory, strategy, enforcementOrderRepository)
                .verify(strategy).process(processedEntityCaptor.capture(), eq(enforcementOrder));
        inOrder(strategyFactory, strategy, enforcementOrderRepository)
                .verify(enforcementOrderRepository).save(processedEntityCaptor.getValue());

        EnforcementOrderEntity savedEntity = processedEntityCaptor.getValue();
        assertThat(savedEntity.getEnforcementOrder()).isEqualTo(enforcementOrder);
        verify(draftCaseDataService).deleteUnsubmittedCaseData(anyLong(), any(EventId.class));
    }

    private static Stream<Arguments> enforcementOrderTypes() {
        return Stream.of(
                arguments(WARRANT),
                arguments(WRIT)
        );
    }

}
