package uk.gov.hmcts.reform.pcs.ccd.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class CaseFileCategoryTest {

    @ParameterizedTest
    @EnumSource(CaseFileCategory.class)
    void shouldReturnLabelForKnownCategoryId(CaseFileCategory category) {
        assertThat(CaseFileCategory.labelFor(category.getId())).isEqualTo(category.getLabel());
    }

    @Test
    void shouldReturnIdWhenCategoryIsUnknown() {
        assertThat(CaseFileCategory.labelFor("unknownFolder")).isEqualTo("unknownFolder");
    }

    @Test
    void shouldReturnNullWhenIdIsNull() {
        assertThat(CaseFileCategory.labelFor(null)).isNull();
    }
}
