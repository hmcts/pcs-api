package uk.gov.hmcts.reform.pcs.enforcement.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.RiskProfileEntity;
import uk.gov.hmcts.reform.pcs.enforcement.service.mapper.RiskDetailsMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementDataUtil.createExpectedRiskProfileEntity;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RiskProfileServiceTest {

    @Mock
    private RiskDetailsMapper riskProfileMapper;
    @InjectMocks
    private RiskProfileService underTest;

    @Test
    void shouldProcessRiskDetailsSuccessfully() {
        // Given
        EnforcementOrderEntity enforcementOrderEntity = new EnforcementOrderEntity();

        EnforcementOrder enforcementOrder = mock(EnforcementOrder.class);
        RiskProfileEntity riskProfileEntity = createExpectedRiskProfileEntity(enforcementOrderEntity);
        when(riskProfileMapper.toEntity(any(), any())).thenReturn(riskProfileEntity);

        // When
        underTest.processRisk(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(enforcementOrderEntity.getRiskProfileDetails()).isEqualTo(riskProfileEntity);
    }
}