package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.confirmeviction.EvictionEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.EvictionRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.strategy.EnforcementTypeStrategy;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.strategy.EnforcementTypeStrategyFactory;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType.WARRANT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType.getSelectEnforcementTypeFromName;

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
    @Mock
    private EvictionRepository evictionRepository;
    @InjectMocks
    private EnforcementOrderService underTest;

    @Captor
    private ArgumentCaptor<EnforcementOrderEntity> enforcementOrderEntityCaptor;

    private final UUID claimId = UUID.fromString("a1b2c3d4-e5f6-7890-1234-567890abcdef");
    private final UUID pcsCaseId = UUID.fromString("7b9e0f1a-2b3c-4d5e-6f7a-8b9c0d1e2f3a");

    private static final long CASE_REFERENCE = 1234L;

    @Test
    void shouldReturnEnforcementOrderIfFoundInDatabase() {
        // Given
        final PcsCaseEntity pcsCaseEntity = EnforcementDataUtil.buildPcsCaseEntity(pcsCaseId, claimId);
        ClaimEntity claimEntity = new ClaimEntity();
        pcsCaseEntity.setClaims(List.of(claimEntity));
        EnforcementOrderEntity enforcementOrderEntity = new EnforcementOrderEntity();
        enforcementOrderEntity.setClaim(claimEntity);
        EnforcementOrder enforcementOrder = EnforcementDataUtil.buildEnforcementOrder();
        enforcementOrderEntity.setEnforcementOrder(enforcementOrder);
        claimEntity.setEnforcementOrders(Set.of(enforcementOrderEntity));
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        // When
        EnforcementOrder retOrder = underTest.retrieveEnforcementOrder(CASE_REFERENCE, WARRANT);

        // Then
        assertThat(retOrder).isNotNull();
    }

    @Test
    void shouldReturnNullIfNotFoundInDatabase() {
        // Given
        final PcsCaseEntity pcsCaseEntity = EnforcementDataUtil.buildPcsCaseEntity(pcsCaseId, claimId);
        ClaimEntity claimEntity = new ClaimEntity();
        pcsCaseEntity.setClaims(List.of(claimEntity));
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);

        // When
        EnforcementOrder retOrder = underTest.retrieveEnforcementOrder(CASE_REFERENCE, WARRANT);

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
        ClaimEntity result = underTest.retrieveClaimEntity(pcsCaseEntity);

        // Then
        assertThat(result).isSameAs(firstClaim);
    }

    @Test
    void shouldReturnNullWhenNoClaimEntitiesPresent() {
        // Given
        final PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setClaims(List.of());

        // When
        ClaimEntity result = underTest.retrieveClaimEntity(pcsCaseEntity);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullWhenNoClaimEntityOnRetrieveEnforcementOrder() {
        // Given
        final PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setClaims(List.of());
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);

        // When
        EnforcementOrder result = underTest.retrieveEnforcementOrder(CASE_REFERENCE, WARRANT);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldThrowWhenSavingWithNoClaimEntityPresent() {
        // Given
        final PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setClaims(List.of());
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);

        // When / Then
        assertThatThrownBy(
            () -> underTest.saveAndClearDraftData(CASE_REFERENCE, new EnforcementOrder())
        ).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("no claim entity exists");
    }

    @Test
    void shouldSaveNewSubmittedEnforcementData() {
        // Given
        final PcsCaseEntity pcsCaseEntity = EnforcementDataUtil.buildPcsCaseEntity(pcsCaseId, claimId);
        final EnforcementOrder enforcementOrder = EnforcementDataUtil.buildEnforcementOrder();

        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(enforcementOrderRepository.save(any())).thenReturn(new EnforcementOrderEntity());
        when(strategyFactory.getStrategy(getSelectEnforcementTypeFromName(
                enforcementOrder.getChooseEnforcementType().getValueCode())))
                .thenReturn(mock(EnforcementTypeStrategy.class));

        // When
        underTest.saveAndClearDraftData(CASE_REFERENCE, enforcementOrder);

        // Then
        verify(enforcementOrderRepository).save(enforcementOrderEntityCaptor.capture());
        EnforcementOrderEntity savedEntity = enforcementOrderEntityCaptor.getValue();
        assertThat(savedEntity.getEnforcementOrder()).isEqualTo(enforcementOrder);
        verify(draftCaseDataService).deleteUnsubmittedCaseData(anyLong(), any(EventId.class));
    }

    @Test
    void shouldConfirmEviction() {
        // Given
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);
        when(pcsCaseService.loadCase(anyLong())).thenReturn(pcsCaseEntity);
        ClaimEntity claimEntity = mock(ClaimEntity.class);
        when(pcsCaseEntity.getClaims()).thenReturn(List.of(claimEntity));
        EnforcementOrderEntity enforcementOrderEntity = mock(EnforcementOrderEntity.class);
        when(claimEntity.getEnforcementOrders()).thenReturn(Set.of(enforcementOrderEntity));

        // When
        underTest.confirmEviction(CASE_REFERENCE);

        // Then
        verify(evictionRepository).save(any(EvictionEntity.class));
    }

    @Test
    void shouldReturnEmptySetWhenNoClaimEntityFound() {
        // Given
        final PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setClaims(List.of());
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);

        // When
        Set<EnforcementOrderEntity> result = underTest.getEnforcementOrderEntities(CASE_REFERENCE);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEnforcementOrdersFromClaimEntity() {
        // Given
        final PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        ClaimEntity claimEntity = new ClaimEntity();
        EnforcementOrderEntity enforcementOrderEntity1 = new EnforcementOrderEntity();
        EnforcementOrderEntity enforcementOrderEntity2 = new EnforcementOrderEntity();
        Set<EnforcementOrderEntity> enforcementOrders = Set.of(enforcementOrderEntity1, enforcementOrderEntity2);
        claimEntity.setEnforcementOrders(enforcementOrders);
        pcsCaseEntity.setClaims(List.of(claimEntity));
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);

        // When
        Set<EnforcementOrderEntity> result = underTest.getEnforcementOrderEntities(CASE_REFERENCE);

        // Then
        assertThat(result).containsExactlyInAnyOrder(enforcementOrderEntity1, enforcementOrderEntity2);
    }

}
