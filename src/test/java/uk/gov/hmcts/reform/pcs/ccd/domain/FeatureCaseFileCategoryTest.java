package uk.gov.hmcts.reform.pcs.ccd.domain;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class FeatureCaseFileCategoryTest {

    @Test
    void shouldDefineTheEnforcementFolderBeforeUncategorisedDocuments() {
        assertThat(FeatureCaseFileCategory.ENFORCEMENT.getId()).isEqualTo("enforcement");
        assertThat(FeatureCaseFileCategory.ENFORCEMENT.getLabel()).isEqualTo("Enforcement");
        assertThat(FeatureCaseFileCategory.ENFORCEMENT.getDisplayOrder())
            .isGreaterThan(CaseFileCategory.CORRESPONDENCE.getDisplayOrder())
            .isLessThan(CaseFileCategory.UNCATEGORISED_DOCUMENTS.getDisplayOrder());
    }

    @Test
    void shouldNotShareAnIdOrDisplayOrderWithAnyCaseFileCategory() {
        List<String> ids = Stream.concat(
            Arrays.stream(CaseFileCategory.values()).map(CaseFileCategory::getId),
            Arrays.stream(FeatureCaseFileCategory.values()).map(FeatureCaseFileCategory::getId)
        ).toList();
        List<Integer> displayOrders = Stream.concat(
            Arrays.stream(CaseFileCategory.values()).map(CaseFileCategory::getDisplayOrder),
            Arrays.stream(FeatureCaseFileCategory.values()).map(FeatureCaseFileCategory::getDisplayOrder)
        ).toList();

        assertThat(ids).doesNotHaveDuplicates();
        assertThat(displayOrders).doesNotHaveDuplicates();
    }
}
