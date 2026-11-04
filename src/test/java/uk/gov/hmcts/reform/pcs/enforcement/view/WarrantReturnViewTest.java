package uk.gov.hmcts.reform.pcs.enforcement.view;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.LatestWarrantReturn;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnName;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnOutcome;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnType;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WarrantReturnEntity;
import uk.gov.hmcts.reform.pcs.enforcement.service.WarrantReturnService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarrantReturnViewTest {

    @Mock
    private WarrantReturnService warrantReturnService;

    @InjectMocks
    private WarrantReturnView underTest;

    private final PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();

    @Test
    void shouldShowTheLatestReturnInUkTime() {
        when(warrantReturnService.findLatestReturn(pcsCaseEntity)).thenReturn(Optional.of(
            WarrantReturnEntity.builder()
                .returnType(WarrantReturnType.FINAL)
                .outcome(WarrantReturnOutcome.EVICTION_SUCCESSFUL)
                .returnName(WarrantReturnName.EVICTION_SUCCESSFUL)
                .evictionAt(Instant.parse("2026-07-08T08:15:00Z"))
                .additionalDetails("Keys handed over")
                .build()));
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setLatestWarrantReturn(pcsCaseEntity, pcsCase);

        assertThat(pcsCase.getEnforcementOrder().getLatestWarrantReturn()).isEqualTo(LatestWarrantReturn.builder()
            .type(WarrantReturnType.FINAL)
            .name("Eviction successful")
            .outcome(WarrantReturnOutcome.EVICTION_SUCCESSFUL)
            .evictionDateTime(LocalDateTime.of(2026, 7, 8, 9, 15))
            .additionalDetails("Keys handed over")
            .build());
    }

    @Test
    void shouldShowAnInterimReturnWithoutOutcomeOrEvictionTime() {
        when(warrantReturnService.findLatestReturn(pcsCaseEntity)).thenReturn(Optional.of(
            WarrantReturnEntity.builder()
                .returnType(WarrantReturnType.INTERIM)
                .returnName(WarrantReturnName.CROWDS_OR_PROTESTS)
                .build()));
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setLatestWarrantReturn(pcsCaseEntity, pcsCase);

        LatestWarrantReturn latest = pcsCase.getEnforcementOrder().getLatestWarrantReturn();
        assertThat(latest.getName()).isEqualTo("Crowds or protests");
        assertThat(latest.getOutcome()).isNull();
        assertThat(latest.getEvictionDateTime()).isNull();
    }

    @Test
    void shouldClearAStaleReturnWhenThereIsNone() {
        when(warrantReturnService.findLatestReturn(pcsCaseEntity)).thenReturn(Optional.empty());
        PCSCase pcsCase = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                                  .latestWarrantReturn(LatestWarrantReturn.builder().name("old").build())
                                  .build())
            .build();

        underTest.setLatestWarrantReturn(pcsCaseEntity, pcsCase);

        assertThat(pcsCase.getEnforcementOrder().getLatestWarrantReturn()).isNull();
    }

    @Test
    void shouldLeaveTheCaseWithoutAnEnforcementOrderWhenThereIsNoReturn() {
        when(warrantReturnService.findLatestReturn(pcsCaseEntity)).thenReturn(Optional.empty());
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setLatestWarrantReturn(pcsCaseEntity, pcsCase);

        assertThat(pcsCase.getEnforcementOrder()).isNull();
    }

    @Test
    void shouldDoNothingWithoutACase() {
        underTest.setLatestWarrantReturn(null, PCSCase.builder().build());
        underTest.setLatestWarrantReturn(pcsCaseEntity, null);

        verifyNoInteractions(warrantReturnService);
    }
}
