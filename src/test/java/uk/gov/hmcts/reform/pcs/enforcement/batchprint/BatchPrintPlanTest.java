package uk.gov.hmcts.reform.pcs.enforcement.batchprint;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BatchPrintPlanTest {

    @Test
    void shouldMoveDroppedCasesFromPrintedToExcludedKeepingTheOrder() {
        PrintableCase one = new PrintableCase("1", "WP-1", List.of(), List.of());
        PrintableCase two = new PrintableCase("2", "WP-2", List.of(), List.of());
        ExcludedCase notMine = ExcludedCase.of("3", BatchPrintExclusion.NOT_ALLOCATED);
        ExcludedCase unavailable = ExcludedCase.of("1", BatchPrintExclusion.DOCUMENT_UNAVAILABLE);

        BatchPrintPlan plan = new BatchPrintPlan(List.of(one, two), List.of(notMine)).exclude(List.of(unavailable));

        assertThat(plan.included()).containsExactly(two);
        assertThat(plan.excluded()).containsExactly(notMine, unavailable);
    }

    @Test
    void shouldMapEachDocumentTypeToItsKind() {
        assertThat(BatchPrintDocumentKind.of(DocumentType.WARRANT_OF_POSSESSION))
            .contains(BatchPrintDocumentKind.WARRANT);
        assertThat(BatchPrintDocumentKind.of(DocumentType.WARRANT_OF_RESTITUTION))
            .contains(BatchPrintDocumentKind.WARRANT);
        assertThat(BatchPrintDocumentKind.of(DocumentType.RISK_ASSESSMENT))
            .contains(BatchPrintDocumentKind.RISK_ASSESSMENT);
        assertThat(BatchPrintDocumentKind.of(DocumentType.NOTICE_OF_EVICTION))
            .contains(BatchPrintDocumentKind.NOTICE_OF_EVICTION);
        assertThat(BatchPrintDocumentKind.of(DocumentType.POLICE_REPORT))
            .isEmpty();
        assertThat(BatchPrintDocumentKind.of(DocumentType.WRIT_OF_POSSESSION))
            .isEmpty();
    }
}
