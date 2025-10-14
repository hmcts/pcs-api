package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.util.PostcodeValidator;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressValidatorTest {

    private static final String TEST_TOWN = "some town";
    private static final String VALID_POSTCODE = "some postcode";
    private static final String INVALID_POSTCODE = "some invalid postcode";

    @Mock(strictness = LENIENT)
    private PostcodeValidator postcodeValidator;

    private AddressValidator underTest;

    @BeforeEach
    void setUp() {
        when(postcodeValidator.isValidPostcode(VALID_POSTCODE)).thenReturn(true);

        underTest = new AddressValidator(postcodeValidator);
    }

    @ParameterizedTest
    @MethodSource("addressScenarios")
    void shouldReturnErrorsIfAddressNotValid(String postTown,
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
            arguments(TEST_TOWN, VALID_POSTCODE, List.of()),
            arguments(null, null, List.of("Town or City is required", "Postcode is required")),
            arguments("", "", List.of("Town or City is required", "Postcode is required")),
            arguments(" ", " ", List.of("Town or City is required", "Postcode is required")),
            arguments(null, VALID_POSTCODE, List.of("Town or City is required")),
            arguments("", VALID_POSTCODE, List.of("Town or City is required")),
            arguments(" ", VALID_POSTCODE, List.of("Town or City is required")),
            arguments(TEST_TOWN, null, List.of("Postcode is required")),
            arguments(TEST_TOWN, "", List.of("Postcode is required")),
            arguments(TEST_TOWN, " ", List.of("Postcode is required")),
            arguments(TEST_TOWN, INVALID_POSTCODE, List.of("Enter a valid postcode")),
            arguments(null, INVALID_POSTCODE, List.of("Town or City is required", "Enter a valid postcode"))
        );
    }

}
