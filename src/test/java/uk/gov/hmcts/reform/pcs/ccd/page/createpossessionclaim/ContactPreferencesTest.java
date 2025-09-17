package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ContactPreferences;
import uk.gov.hmcts.reform.pcs.ccd.service.AddressValidator;
import uk.gov.hmcts.reform.pcs.ccd.util.PostcodeValidator;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactPreferencesTest extends BasePageTest {

    @Mock
    private AddressValidator addressValidator;
    @Mock
    private PostcodeValidator postcodeValidator;

    @BeforeEach
    void setUp() {
        setPageUnderTest(new ContactPreferences(addressValidator, postcodeValidator));
    }

    @Test
    void shouldReturnValidationErrorsWhenAddressInvalid() {
        // Given
        AddressUK contactAddress = AddressUK.builder()
            .addressLine1("123 Test Street")
            .postTown("Test City")
            .postCode("M1 1AA")
            .build();

        PCSCase caseData = PCSCase.builder()
            .isCorrectClaimantContactAddress(VerticalYesNo.NO)
            .overriddenClaimantContactAddress(contactAddress)
            .build();

        List<String> expectedValidationErrors = List.of("Address line 1 is required");
        when(addressValidator.validateAddressFields(contactAddress)).thenReturn(expectedValidationErrors);
        when(postcodeValidator.getValidationErrors(contactAddress, "overriddenClaimantContactAddress"))
            .thenReturn(List.of());

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isEqualTo(expectedValidationErrors);
    }

    @Test
    void shouldReturnPostcodeValidationErrors() {
        // Given
        AddressUK contactAddress = AddressUK.builder()
            .addressLine1("123 Test Street")
            .postTown("Test City")
            .postCode("12345") // Invalid postcode
            .build();

        PCSCase caseData = PCSCase.builder()
            .isCorrectClaimantContactAddress(VerticalYesNo.NO)
            .overriddenClaimantContactAddress(contactAddress)
            .build();

        when(addressValidator.validateAddressFields(contactAddress)).thenReturn(List.of());
        when(postcodeValidator.getValidationErrors(contactAddress, "overriddenClaimantContactAddress"))
            .thenReturn(List.of("Enter a valid postcode"));

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).containsExactly("Enter a valid postcode");
    }

    @Test
    void shouldReturnCombinedValidationErrors() {
        // Given - Both address and postcode validation errors
        AddressUK contactAddress = AddressUK.builder()
            .addressLine1("123 Test Street")
            .postTown("Test City")
            .postCode("12345") // Invalid postcode
            .build();

        PCSCase caseData = PCSCase.builder()
            .isCorrectClaimantContactAddress(VerticalYesNo.NO)
            .overriddenClaimantContactAddress(contactAddress)
            .build();

        List<String> addressErrors = List.of("Address line 1 is required");
        List<String> postcodeErrors = List.of("Enter a valid postcode");
        
        when(addressValidator.validateAddressFields(contactAddress)).thenReturn(addressErrors);
        when(postcodeValidator.getValidationErrors(contactAddress, "overriddenClaimantContactAddress"))
            .thenReturn(postcodeErrors);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).containsExactlyInAnyOrder(
            "Address line 1 is required",
            "Enter a valid postcode"
        );
    }

    @Test
    void shouldHandleNullOverriddenClaimantContactAddress() {
        // Given - When address is null but user says it's not correct
        PCSCase caseData = PCSCase.builder()
            .isCorrectClaimantContactAddress(VerticalYesNo.NO)
            .overriddenClaimantContactAddress(null)
            .build();

        when(addressValidator.validateAddressFields(null)).thenReturn(List.of("Address is required"));
        when(postcodeValidator.getValidationErrors(null, "overriddenClaimantContactAddress"))
            .thenReturn(List.of("Postcode is required"));

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).containsExactlyInAnyOrder(
            "Address is required",
            "Postcode is required"
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"M1AA", "M1A", "M1A1A1A1", "1MAA", "M", ""})
    void shouldReturnErrorForInvalidPostcodeFormats(String invalidPostcode) {
        // Given
        AddressUK overriddenAddress = AddressUK.builder()
            .addressLine1("123 Test Street")
            .postTown("Test City")
            .postCode(invalidPostcode)
            .build();

        PCSCase caseData = PCSCase.builder()
            .isCorrectClaimantContactAddress(VerticalYesNo.NO)
            .overriddenClaimantContactAddress(overriddenAddress)
            .build();

        when(addressValidator.validateAddressFields(overriddenAddress)).thenReturn(List.of());
        when(postcodeValidator.getValidationErrors(overriddenAddress, "overriddenClaimantContactAddress"))
            .thenReturn(List.of("Enter a valid postcode"));

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).containsExactly("Enter a valid postcode");
    }

    @Test
    void shouldHandleMultipleValidationErrors() {
        // Given
        AddressUK overriddenAddress = AddressUK.builder()
            .addressLine1("123 Test Street")
            .postTown("Test City")
            .postCode("12345") // Invalid postcode
            .build();

        PCSCase caseData = PCSCase.builder()
            .isCorrectClaimantContactAddress(VerticalYesNo.NO)
            .overriddenClaimantContactAddress(overriddenAddress)
            .build();

        when(addressValidator.validateAddressFields(overriddenAddress)).thenReturn(List.of());
        when(postcodeValidator.getValidationErrors(overriddenAddress, "overriddenClaimantContactAddress"))
            .thenReturn(List.of(
                "Enter a valid postcode",
                "Additional validation error"
            ));

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).containsExactly(
            "Enter a valid postcode",
            "Additional validation error"
        );
    }
}