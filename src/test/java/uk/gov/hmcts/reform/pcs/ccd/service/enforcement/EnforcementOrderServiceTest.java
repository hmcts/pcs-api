package uk.gov.hmcts.reform.pcs.ccd.service.enforcement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcement.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcement.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.exception.EnforcementOrderNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnforcementOrderServiceTest {

    @Mock
    private EnforcementOrderRepository enfCaseRepository;

    @Mock
    private PcsCaseRepository pcsCaseRepository;

    @InjectMocks
    private EnforcementOrderService enforcementOrderService;

    private UUID enforcementOrderId;

    private UUID claimId;

    private UUID pcsCaseId;

    static final long CASE_REFERENCE = 1234L;

    @BeforeEach
    void setUp() {
        enforcementOrderId = UUID.randomUUID();
        claimId = UUID.randomUUID();
        pcsCaseId = UUID.randomUUID();
    }

    @Test
    void shouldThrowExceptionWhenNoEnforcementOrder() {
        // Given
        when(enfCaseRepository.findById(enforcementOrderId)).thenReturn(Optional.empty());

        // When &
        // Then
        assertThatThrownBy(() -> enforcementOrderService.loadEnforcementOrder(enforcementOrderId))
                .isInstanceOf(EnforcementOrderNotFoundException.class)
                .hasMessageContaining("No enforcement order found for case reference");
    }

    @Test
    void shouldReturnEnforcementOrderWhenFound() {
        // Given
        final PcsCaseEntity pcsCaseEntity = EnforcementDataUtil.buildPcsCaseEntity(pcsCaseId, claimId);
        final ClaimEntity claimEntity = pcsCaseEntity.getClaims().iterator().next();
        final EnforcementOrder enforcementOrder = EnforcementDataUtil.buildEnforcementOrder();
        final EnforcementOrderEntity enforcementOrderEntity =
                EnforcementDataUtil.buildEnforcementOrderEntity(enforcementOrderId, claimEntity, enforcementOrder);
        when(enfCaseRepository.findById(enforcementOrderId)).thenReturn(Optional.of(enforcementOrderEntity));

        // When
        EnforcementOrderEntity retrievedEnforcementOrderEntity =
                enforcementOrderService.loadEnforcementOrder(enforcementOrderId);

        // Then
        assertThat(retrievedEnforcementOrderEntity.getEnforcementOrder()).isEqualTo(enforcementOrder);
    }

    @Test
    void shouldSaveNewSubmittedEnforcementData() {
        // Given
        final PcsCaseEntity pcsCaseEntity = EnforcementDataUtil.buildPcsCaseEntity(pcsCaseId, claimId);
        final EnforcementOrder enforcementOrder = EnforcementDataUtil.buildEnforcementOrder();

        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE))
                .thenReturn(Optional.of(pcsCaseEntity));

        // When
        enforcementOrderService.createEnforcementOrder(CASE_REFERENCE, enforcementOrder);

        // Then
        verify(enfCaseRepository).save(any());
    }
}