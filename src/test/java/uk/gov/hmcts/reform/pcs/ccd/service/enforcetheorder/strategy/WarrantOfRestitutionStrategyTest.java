package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.strategy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WarrantOfRestitutionEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.WarrantOfRestitutionRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class WarrantOfRestitutionStrategyTest {

    @InjectMocks
    private WarrantOfRestitutionStrategy underTest;
    @Mock
    private WarrantOfRestitutionRepository warrantOfRestitutionRepository;

    @Test
    void shouldProcess() {
        // Given
        EnforcementOrderEntity enforcementOrderEntity = new EnforcementOrderEntity();
        EnforcementOrder enforcementOrder = new EnforcementOrder();

        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);
        ArgumentCaptor<WarrantOfRestitutionEntity> captor =
            ArgumentCaptor.forClass(WarrantOfRestitutionEntity.class);

        // Then
        verify(warrantOfRestitutionRepository).save(captor.capture());
        WarrantOfRestitutionEntity saved = captor.getValue();
        assertThat(saved).isNotNull();
        assertThat(saved.getEnforcementOrder()).isSameAs(enforcementOrderEntity);
    }

}
