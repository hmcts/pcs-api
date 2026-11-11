package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.strategy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WritEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper.WritDetailsMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WritStrategyTest {

    @Mock
    private WritDetailsMapper writDetailsMapper;

    @InjectMocks
    private WritStrategy underTest;

    @Test
    void shouldMapWritDetailsAndLinkEntitiesBidirectionally() {
        // Given
        EnforcementOrderEntity enforcementOrderEntity = new EnforcementOrderEntity();
        WritDetails writDetails = WritDetails.builder().build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().writDetails(writDetails).build();
        WritEntity writEntity = new WritEntity();
        when(writDetailsMapper.toEntity(writDetails)).thenReturn(writEntity);

        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);

        // Then
        verify(writDetailsMapper).toEntity(writDetails);
        assertThat(enforcementOrderEntity.getWritDetails()).isSameAs(writEntity);
        assertThat(writEntity.getEnfApplication()).isSameAs(enforcementOrderEntity);
    }

}
