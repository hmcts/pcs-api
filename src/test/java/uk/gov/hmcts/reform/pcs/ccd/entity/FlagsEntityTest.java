package uk.gov.hmcts.reform.pcs.ccd.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FlagsEntityTest {

    private FlagsEntity underTest;

    @BeforeEach
    void setUp() {
        underTest = new FlagsEntity();
    }

    @Test
    void shouldUpdateFlagsEntity() {
        // Given
        FlagsEntity flagsEntity = FlagsEntity.builder().build();
        FlagDetailsEntity flagDetailsEntity = FlagDetailsEntity.builder()
            .flagCode("FLAG_CODE")
            .caseFlag(flagsEntity)
            .name("FLAG_NAME")
            .flagComment("FLAG_COMMENT")
            .build();

        // When
        underTest.setCaseFlags(List.of(flagDetailsEntity));

        // Then
        assertThat(underTest.getCaseFlags()).containsExactly(flagDetailsEntity);
        assertThat(underTest.getCaseFlags().getFirst().getFlagCode()).isEqualTo("FLAG_CODE");

    }
}
