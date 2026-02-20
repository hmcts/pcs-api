package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.warrant.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.strategy.EnforcementTypeStrategy;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.strategy.EnforcementTypeStrategyFactory;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.warrant.EnforcementOrderService;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnforcementOrderServiceTest {

    @Mock
    private DraftCaseDataService draftCaseDataService;
    @Mock
    private EnforcementOrderRepository enforcementOrderRepository;
    @Mock
    private PcsCaseRepository pcsCaseRepository;
    @Mock
    private EnforcementTypeStrategyFactory strategyFactory;
    @InjectMocks
    private EnforcementOrderService enforcementOrderService;

    @Captor
    private ArgumentCaptor<EnforcementOrderEntity> enforcementOrderEntityCaptor;

    private final UUID enforcementOrderId = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479");
    private final UUID claimId = UUID.fromString("a1b2c3d4-e5f6-7890-1234-567890abcdef");
    private final UUID pcsCaseId = UUID.fromString("7b9e0f1a-2b3c-4d5e-6f7a-8b9c0d1e2f3a");

    private static final long CASE_REFERENCE = 1234L;

    @Test
    void shouldSaveNewSubmittedEnforcementData() {
        // Given
        final PcsCaseEntity pcsCaseEntity = EnforcementDataUtil.buildPcsCaseEntity(pcsCaseId, claimId);
        final EnforcementOrder enforcementOrder = EnforcementDataUtil.buildEnforcementOrder();

        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE))
            .thenReturn(Optional.of(pcsCaseEntity));
        when(enforcementOrderRepository.save(any())).thenReturn(new EnforcementOrderEntity());
        when(strategyFactory.getStrategy(enforcementOrder.getSelectEnforcementType()))
            .thenReturn(mock(EnforcementTypeStrategy.class));

        // When
        enforcementOrderService.saveAndClearDraftData(CASE_REFERENCE, enforcementOrder);

        // Then
        verify(enforcementOrderRepository).save(enforcementOrderEntityCaptor.capture());
        EnforcementOrderEntity savedEntity = enforcementOrderEntityCaptor.getValue();
        assertThat(savedEntity.getEnforcementOrder()).isEqualTo(enforcementOrder);
        verify(draftCaseDataService).deleteUnsubmittedCaseData(anyLong(), any(EventId.class));
    }

}
