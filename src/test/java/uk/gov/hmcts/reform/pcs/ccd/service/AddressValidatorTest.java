package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class AddressValidatorTest {

    private static final String TEST_TOWN = "some town";
    private static final String TEST_POSTCODE = "some postcode";
    private AddressValidator underTest;

    @BeforeEach
    void setUp() {
        underTest = new AddressValidator();
    }

    @ParameterizedTest
    @MethodSource("addressScenarios")
    void shouldReturnNoErrorsWhenTownAndPostcodeNotBlank(String postTown,
                                                         String postcode,
                                                         List<String> expectedValidationErrors) {
        // Given
        AddressUK address = AddressUK.builder()
            .postTown(postTown)
            .postCode(postcode)
            .build();

        // When
        List<String> actualValidationErrors = underTest.validateAddressFields(address);

        // Then
        assertThat(actualValidationErrors).isEqualTo(expectedValidationErrors);
    }

    private static Stream<Arguments> addressScenarios() {
        return Stream.of(
            // Town, postcode, expected validation errors
            arguments(TEST_TOWN, TEST_POSTCODE, List.of()),
            arguments(null, null, List.of("Town or City is required", "Postcode is required")),
            arguments("", "", List.of("Town or City is required", "Postcode is required")),
            arguments(" ", " ", List.of("Town or City is required", "Postcode is required")),
            arguments(null, TEST_POSTCODE, List.of("Town or City is required")),
            arguments("", TEST_POSTCODE, List.of("Town or City is required")),
            arguments(" ", TEST_POSTCODE, List.of("Town or City is required")),
            arguments(TEST_TOWN, null, List.of("Postcode is required")),
            arguments(TEST_TOWN, "", List.of("Postcode is required")),
            arguments(TEST_TOWN, " ", List.of("Postcode is required"))
        );
    }

}
