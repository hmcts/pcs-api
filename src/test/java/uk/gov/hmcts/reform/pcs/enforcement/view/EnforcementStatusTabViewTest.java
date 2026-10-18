package uk.gov.hmcts.reform.pcs.enforcement.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnName;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnType;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WarrantReturnEntity;
import uk.gov.hmcts.reform.pcs.enforcement.service.CurrentEnforcementService;
import uk.gov.hmcts.reform.pcs.enforcement.service.WarrantReturnService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnforcementStatusTabViewTest {

    @Mock
    private CurrentEnforcementService currentEnforcementService;
    @Mock
    private WarrantReturnService warrantReturnService;

    private EnforcementStatusTabView underTest;
    private final PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();

    @BeforeEach
    void setUp() {
        underTest = new EnforcementStatusTabView(currentEnforcementService, warrantReturnService);
        lenient().when(warrantReturnService.findLatestReturn(any(EnforcementOrderEntity.class)))
            .thenReturn(Optional.empty());
    }

    @Test
    void shouldLeaveTheTabHiddenBeforeAnyWarrantOrWritIsGenerated() {
        when(currentEnforcementService.findEnforcementsCurrentFirst(pcsCaseEntity)).thenReturn(List.of(
            enforcement("WP-1", EnforcementStatus.AWAITING_REVIEW, null),
            enforcement(null, EnforcementStatus.REJECTED, null)));
        PCSCase pcsCase = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder().enforcementStatusTabMarkdown("stale").build())
            .build();

        underTest.setEnforcementStatusTab(pcsCaseEntity, pcsCase);

        assertThat(pcsCase.getEnforcementOrder().getEnforcementStatusTabMarkdown()).isNull();
    }

    @Test
    void shouldNotAddAnEnforcementOrderToACaseWithNoEnforcement() {
        when(currentEnforcementService.findEnforcementsCurrentFirst(pcsCaseEntity)).thenReturn(List.of());
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setEnforcementStatusTab(pcsCaseEntity, pcsCase);

        assertThat(pcsCase.getEnforcementOrder()).isNull();
    }

    @Test
    void shouldListOnlyGeneratedWarrantsAndWritsNewestFirst() {
        when(currentEnforcementService.findEnforcementsCurrentFirst(pcsCaseEntity)).thenReturn(List.of(
            enforcement("WP-00000003", EnforcementStatus.AWAITING_PAYMENT, null),
            enforcement("WP-00000001", EnforcementStatus.CLOSED, Instant.parse("2026-02-01T10:00:00Z")),
            enforcement("WR-00000002", EnforcementStatus.AWAITING_SCHEDULING, Instant.parse("2026-08-01T10:00:00Z"))));
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setEnforcementStatusTab(pcsCaseEntity, pcsCase);

        String markdown = pcsCase.getEnforcementOrder().getEnforcementStatusTabMarkdown();
        assertThat(markdown).doesNotContain("WP-00000003");
        assertThat(markdown.indexOf("WR-00000002")).isPositive().isLessThan(markdown.indexOf("WP-00000001"));
    }

    @Test
    void shouldShowEachWarrantsOwnLatestReturn() {
        EnforcementOrderEntity closed = enforcement("WP-00000001", EnforcementStatus.CLOSED,
                                                    Instant.parse("2026-02-01T10:00:00Z"));
        when(currentEnforcementService.findEnforcementsCurrentFirst(pcsCaseEntity)).thenReturn(List.of(closed));
        when(warrantReturnService.findLatestReturn(closed)).thenReturn(Optional.of(WarrantReturnEntity.builder()
            .returnType(WarrantReturnType.FINAL)
            .returnName(WarrantReturnName.WARRANT_WITHDRAWN)
            .build()));
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setEnforcementStatusTab(pcsCaseEntity, pcsCase);

        assertThat(pcsCase.getEnforcementOrder().getEnforcementStatusTabMarkdown())
            .contains(WarrantReturnName.WARRANT_WITHDRAWN.getLabel())
            .doesNotContain("No return recorded yet");
    }

    @Test
    void shouldRenderTheDefendantsTabFromTheGivenWarrantsWithoutTheClaimantsSection() {
        EnforcementOrderEntity served = enforcement("WP-00000001", EnforcementStatus.CLAIMANT_CONFIRMED,
                                                    Instant.parse("2026-02-01T10:00:00Z"));

        String markdown = underTest.defendantTabMarkdown(List.of(served));

        assertThat(markdown).contains("WP-00000001").doesNotContain("Claimant eviction confirmation");
        verifyNoInteractions(currentEnforcementService);
    }

    @Test
    void shouldGiveDefendantsNoTabWhenNoGivenWarrantIsGenerated() {
        assertThat(underTest.defendantTabMarkdown(List.of())).isNull();
        assertThat(underTest.defendantTabMarkdown(
            List.of(enforcement("WP-1", EnforcementStatus.AWAITING_REVIEW, null)))).isNull();
    }

    @Test
    void shouldDoNothingWithoutACase() {
        underTest.setEnforcementStatusTab(null, PCSCase.builder().build());
        underTest.setEnforcementStatusTab(pcsCaseEntity, null);

        verifyNoInteractions(currentEnforcementService, warrantReturnService);
    }

    private static EnforcementOrderEntity enforcement(String number, EnforcementStatus status, Instant approvedAt) {
        return EnforcementOrderEntity.builder()
            .id(UUID.randomUUID())
            .enforcementNumber(number)
            .status(status)
            .approvedAt(approvedAt)
            .build();
    }
}
