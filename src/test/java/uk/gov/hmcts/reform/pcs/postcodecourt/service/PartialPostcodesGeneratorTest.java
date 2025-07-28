package uk.gov.hmcts.reform.pcs.postcodecourt.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class PartialPostcodesGeneratorTest {

    private PartialPostcodesGenerator underTest;

    @BeforeEach
    void setUp() {
        underTest = new PartialPostcodesGenerator();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("postcodeScenarios")
    void shouldGeneratePartialPostcodesByTrimmingInwardCode(String postcode, List<String> expectedPartialPostcodes) {
        List<String> partialPostcodes = underTest.generateForPostcode(postcode);

        assertThat(partialPostcodes).containsExactlyElementsOf(expectedPartialPostcodes);
    }

    private static Stream<Arguments> postcodeScenarios() {
        return Stream.of(
            arguments(" AB12 3CD ", List.of("AB123CD", "AB123C", "AB123", "AB12")),
            arguments("EF4 5DE", List.of("EF45DE", "EF45D", "EF45", "EF4"))
        );
    }
}
