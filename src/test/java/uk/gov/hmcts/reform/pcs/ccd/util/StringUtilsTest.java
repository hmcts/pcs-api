package uk.gov.hmcts.reform.pcs.ccd.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

class StringUtilsTest {

    @ParameterizedTest
    @MethodSource("joinScenarios")
    void joinIfNotEmpty(String delimiter, List<String> elements, String expectedResult) {
        // When
        String actualResult = StringUtils.joinIfNotEmpty(delimiter, elements);

        // Then
        assertThat(actualResult).isEqualTo(expectedResult);
    }

    private static Stream<Arguments> joinScenarios() {
        return Stream.of(
            // Delimiter, List, Expected Result
            argumentSet("Null list",",", null, null),
            argumentSet("Empty list", ",", List.of(), null),
            argumentSet("Single item", ",", List.of("test1"), "test1"),
            argumentSet("Multiple items", ",", List.of("test1", "test2"), "test1,test2"),
            argumentSet("Different delimiter", "\n", List.of("test1", "test2"), "test1\ntest2")
        );
    }

}
