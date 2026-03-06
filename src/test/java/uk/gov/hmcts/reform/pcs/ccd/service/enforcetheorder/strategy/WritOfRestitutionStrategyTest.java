package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.strategy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WritOfRestitutionEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.WritOfRestitutionRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class WritOfRestitutionStrategyTest {

    @InjectMocks
    private WritOfRestitutionStrategy underTest;
    @Mock
    private WritOfRestitutionRepository writOfRestitutionRepository;
    @Mock
    private Clock clock;

    @Test
    void shouldProcess() {
        // Given
        EnforcementOrderEntity enforcementOrderEntity = new EnforcementOrderEntity();
        EnforcementOrder enforcementOrder = new EnforcementOrder();
        when(clock.instant()).thenReturn(Instant.parse("2026-03-05T00:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("Europe/London"));

        // When
        underTest.process(enforcementOrderEntity, enforcementOrder);
        ArgumentCaptor<WritOfRestitutionEntity> captor =
            ArgumentCaptor.forClass(WritOfRestitutionEntity.class);

        // Then
        verify(writOfRestitutionRepository).save(captor.capture());
        WritOfRestitutionEntity saved = captor.getValue();
        assertThat(saved).isNotNull();
        assertThat(saved.getEnforcementOrder()).isSameAs(enforcementOrderEntity);
    }

}
