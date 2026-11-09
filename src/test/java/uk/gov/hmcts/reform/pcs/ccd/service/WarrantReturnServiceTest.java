package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.bailiff.EvictionOutcome;
import uk.gov.hmcts.reform.pcs.ccd.domain.bailiff.WarrantReturn;
import uk.gov.hmcts.reform.pcs.ccd.domain.bailiff.WarrantReturnName;
import uk.gov.hmcts.reform.pcs.ccd.domain.bailiff.WarrantReturnType;
import uk.gov.hmcts.reform.pcs.ccd.entity.WarrantReturnEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WarrantEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.WarrantRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class WarrantReturnServiceTest {

    @Mock
    private Clock utcClock;

    @Mock
    private WarrantRepository warrantRepository;

    @InjectMocks
    WarrantReturnService warrantReturnService;

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-22T21:05:30Z");

    @BeforeEach
    void setUp() {
        when(utcClock.instant()).thenReturn(FIXED_INSTANT);
        when(utcClock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @Test
    void shouldSaveWarrantReturnWithNoPreexistingWarrantReturns() {
        // Given
        WarrantReturn warrantReturn = WarrantReturn.builder()
            .returnName(WarrantReturnName.EVICTION_SUCCESSFUL)
            .returnType(WarrantReturnType.FINAL)
            .evictionOutcome(EvictionOutcome.SUCCESSFUL)
            .additionalDetails("additional details")
            .evictionDate(LocalDateTime.of(2026, 4, 21, 9, 30))
            .build();

        WarrantEntity warrantEntity = WarrantEntity.builder().build();

        // When
        warrantReturnService.addWarrantReturn(warrantEntity, warrantReturn);

        verify(warrantRepository).save(warrantEntity);

        assertThat(warrantEntity.getWarrantReturns()).hasSize(1);

        WarrantReturnEntity warrantReturnEntity = warrantEntity.getWarrantReturns().getFirst();
        assertThat(warrantReturnEntity.getReturnName()).isEqualTo(WarrantReturnName.EVICTION_SUCCESSFUL);
        assertThat(warrantReturnEntity.getReturnType()).isEqualTo(WarrantReturnType.FINAL);
        assertThat(warrantReturnEntity.getOutcome()).isEqualTo(EvictionOutcome.SUCCESSFUL);
        assertThat(warrantReturnEntity.getAdditionalDetails()).isEqualTo("additional details");
        assertThat(warrantReturnEntity.getOutcomeDate()).isEqualTo(LocalDateTime.of(2026, 4, 21, 9, 30));
        assertThat(warrantReturnEntity.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 4, 22, 21, 5, 30));
    }

    @Test
    void shouldSaveWarrantReturnWithPreexistingWarrantReturns() {
        // Given
        WarrantReturn warrantReturn = WarrantReturn.builder()
            .returnName(WarrantReturnName.EVICTION_SUCCESSFUL)
            .returnType(WarrantReturnType.FINAL)
            .evictionOutcome(EvictionOutcome.SUCCESSFUL)
            .additionalDetails("additional details")
            .evictionDate(LocalDateTime.of(2026, 4, 21, 9, 30))
            .build();

        List<WarrantReturnEntity> warrantReturnEntities = new ArrayList<>();
        warrantReturnEntities.add(WarrantReturnEntity.builder().build());
        WarrantEntity warrantEntity = WarrantEntity.builder()
            .warrantReturns(warrantReturnEntities)
            .build();

        // When
        warrantReturnService.addWarrantReturn(warrantEntity, warrantReturn);

        verify(warrantRepository).save(warrantEntity);

        assertThat(warrantEntity.getWarrantReturns()).hasSize(2);

        WarrantReturnEntity warrantReturnEntity = warrantEntity.getWarrantReturns().getLast();
        assertThat(warrantReturnEntity.getReturnName()).isEqualTo(WarrantReturnName.EVICTION_SUCCESSFUL);
        assertThat(warrantReturnEntity.getReturnType()).isEqualTo(WarrantReturnType.FINAL);
        assertThat(warrantReturnEntity.getOutcome()).isEqualTo(EvictionOutcome.SUCCESSFUL);
        assertThat(warrantReturnEntity.getAdditionalDetails()).isEqualTo("additional details");
        assertThat(warrantReturnEntity.getOutcomeDate()).isEqualTo(LocalDateTime.of(2026, 4, 21, 9, 30));
        assertThat(warrantReturnEntity.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 4, 22, 21, 5, 30));
    }

}
