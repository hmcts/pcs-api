package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.RiskProfileEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.RiskProfileRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper.RiskDetailsMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.EnforcementDataUtil.createExpectedRiskProfileEntity;

@ExtendWith(MockitoExtension.class)
class RiskProfileServiceTest {

    @Mock
    private RiskDetailsMapper riskProfileMapper;
    @Mock
    private RiskProfileRepository riskProfileRepository;
    @InjectMocks
    private RiskProfileService underTest;

    @Test
    void shouldProcessRiskDetailsSuccessfully() {
        // Given
        EnforcementOrderEntity enforcementOrderEntity = mock(EnforcementOrderEntity.class);

        EnforcementOrder enforcementOrder = mock(EnforcementOrder.class);
        RiskProfileEntity riskProfileEntity = createExpectedRiskProfileEntity(enforcementOrderEntity);
        when(riskProfileMapper.toEntity(any(), any())).thenReturn(riskProfileEntity);

        // When
        underTest.processRisk(enforcementOrder, enforcementOrderEntity);

        // Then
        verify(riskProfileRepository).save(riskProfileEntity);
    }
}