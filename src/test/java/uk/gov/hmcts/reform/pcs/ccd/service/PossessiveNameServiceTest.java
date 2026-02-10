package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

class PossessiveNameServiceTest {

    private PossessiveNameService underTest;

    @BeforeEach
    void setUp() {
        underTest = new PossessiveNameService();
    }

    @ParameterizedTest
    @MethodSource("nameScenarios")
    @DisplayName("Should return possessive form of name")
    void applyApostrophe(String name, String expectedPossessiveName) {
        // When
        String actualPossessiveName = underTest.applyApostrophe(name);

        // Then
        assertThat(actualPossessiveName).isEqualTo(expectedPossessiveName);
    }

    private static Stream<Arguments> nameScenarios() {
        return Stream.of(
            argumentSet("null name", null, null),
            argumentSet("empty name", "", ""),
            argumentSet("blank name", " ", ""),
            argumentSet("ending in non-S character", "Tom", "Tom’s"),
            argumentSet("name with surrounding space", " Tom ", "Tom’s"),
            argumentSet("quote in middle of name", "O’Brien & Associates", "O’Brien & Associates’"),
            argumentSet("ending with apostrophe", "Name’", "Name’"),
            argumentSet("ending with single character S", "S", "S’"),
            argumentSet("ending with S", "TREETOPS", "TREETOPS’"),
            argumentSet("ending with s", "Treetops", "Treetops’"),
            argumentSet("ending with ’s", "James’s", "James’s"),
            argumentSet("ending with ’S", "JAMES’S", "JAMES’S")
        );
    }

}
