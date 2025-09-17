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
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.DefendantsDetails;
import uk.gov.hmcts.reform.pcs.ccd.util.PostcodeValidator;
import uk.gov.hmcts.reform.pcs.ccd.service.AddressValidator;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefendantsDetailsTest extends BasePageTest {

    @Mock
    private AddressValidator addressValidator;
    @Mock
    private PostcodeValidator postcodeValidator;

    @BeforeEach
    void setUp() {
        setPageUnderTest(new DefendantsDetails(addressValidator, postcodeValidator));
    }

    @Test
    void shouldReturnValidationErrorsWhenAddressInvalid() {
        // Given
        AddressUK correspondenceAddress = AddressUK.builder()
            .addressLine1("123 Test Street")
            .postTown("Test City")
            .postCode("M1 1AA")
            .build();

        DefendantDetails defendantsDetails = DefendantDetails.builder()
            .addressSameAsPossession(VerticalYesNo.NO)
            .addressKnown(VerticalYesNo.YES)
            .correspondenceAddress(correspondenceAddress)
            .build();

        PCSCase caseData = PCSCase.builder()
            .defendant1(defendantsDetails)
            .build();

        List<String> expectedValidationErrors = List.of("Address line 1 is required");
        when(addressValidator.validateAddressFields(correspondenceAddress)).thenReturn(expectedValidationErrors);
        when(postcodeValidator.getValidationErrors(correspondenceAddress, "defendant1.correspondenceAddress"))
            .thenReturn(List.of());

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isEqualTo(expectedValidationErrors);
    }

    @Test
    void shouldReturnPostcodeValidationErrors() {
        // Given
        AddressUK correspondenceAddress = AddressUK.builder()
            .addressLine1("123 Test Street")
            .postTown("Test City")
            .postCode("12345") // Invalid postcode
            .build();

        DefendantDetails defendantsDetails = DefendantDetails.builder()
            .addressSameAsPossession(VerticalYesNo.NO)
            .addressKnown(VerticalYesNo.YES)
            .correspondenceAddress(correspondenceAddress)
            .build();

        PCSCase caseData = PCSCase.builder()
            .defendant1(defendantsDetails)
            .build();

        when(addressValidator.validateAddressFields(correspondenceAddress)).thenReturn(List.of());
        when(postcodeValidator.getValidationErrors(correspondenceAddress, "defendant1.correspondenceAddress"))
            .thenReturn(List.of("Enter a valid postcode"));

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).containsExactly("Enter a valid postcode");
    }

    @Test
    void shouldReturnCombinedValidationErrors() {
        // Given - Both address and postcode validation errors
        AddressUK correspondenceAddress = AddressUK.builder()
            .addressLine1("123 Test Street")
            .postTown("Test City")
            .postCode("12345") // Invalid postcode
            .build();

        DefendantDetails defendantsDetails = DefendantDetails.builder()
            .addressSameAsPossession(VerticalYesNo.NO)
            .addressKnown(VerticalYesNo.YES)
            .correspondenceAddress(correspondenceAddress)
            .build();

        PCSCase caseData = PCSCase.builder()
            .defendant1(defendantsDetails)
            .build();

        List<String> addressErrors = List.of("Address line 1 is required");
        List<String> postcodeErrors = List.of("Enter a valid postcode");

        when(addressValidator.validateAddressFields(correspondenceAddress)).thenReturn(addressErrors);
        when(postcodeValidator.getValidationErrors(correspondenceAddress, "defendant1.correspondenceAddress"))
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
    void shouldNotValidateWhenAddressSameAsPossession() {
        // Given - When addressSameAsPossession is YES, no validation should occur
        DefendantDetails defendantsDetails = DefendantDetails.builder()
            .addressSameAsPossession(VerticalYesNo.YES)
            .addressKnown(VerticalYesNo.YES)
            .correspondenceAddress(null)
            .build();

        PCSCase caseData = PCSCase.builder()
            .defendant1(defendantsDetails)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNullOrEmpty();
    }

    @Test
    void shouldNotValidateWhenAddressNotKnown() {
        // Given - When addressKnown is NO, no validation should occur
        DefendantDetails defendantsDetails = DefendantDetails.builder()
            .addressSameAsPossession(VerticalYesNo.NO)
            .addressKnown(VerticalYesNo.NO)
            .correspondenceAddress(null)
            .build();

        PCSCase caseData = PCSCase.builder()
            .defendant1(defendantsDetails)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNullOrEmpty();
    }

    @Test
    void shouldNotValidateWhenDefendantDetailsIsNull() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .defendant1(null)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNullOrEmpty();
    }

    @Test
    void shouldHandleNullCorrespondenceAddress() {
        // Given - When correspondence address is null but should be validated
        DefendantDetails defendantsDetails = DefendantDetails.builder()
            .addressSameAsPossession(VerticalYesNo.NO)
            .addressKnown(VerticalYesNo.YES)
            .correspondenceAddress(null)
            .build();

        PCSCase caseData = PCSCase.builder()
            .defendant1(defendantsDetails)
            .build();

        when(addressValidator.validateAddressFields(null)).thenReturn(List.of("Address is required"));
        when(postcodeValidator.getValidationErrors(null, "defendant1.correspondenceAddress"))
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
        AddressUK correspondenceAddress = AddressUK.builder()
            .addressLine1("123 Test Street")
            .postTown("Test City")
            .postCode(invalidPostcode)
            .build();

        DefendantDetails defendantsDetails = DefendantDetails.builder()
            .addressSameAsPossession(VerticalYesNo.NO)
            .addressKnown(VerticalYesNo.YES)
            .correspondenceAddress(correspondenceAddress)
            .build();

        PCSCase caseData = PCSCase.builder()
            .defendant1(defendantsDetails)
            .build();

        when(addressValidator.validateAddressFields(correspondenceAddress)).thenReturn(List.of());
        when(postcodeValidator.getValidationErrors(correspondenceAddress, "defendant1.correspondenceAddress"))
            .thenReturn(List.of("Enter a valid postcode"));

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).containsExactly("Enter a valid postcode");
    }

    @Test
    void shouldPassValidationForValidData() {
        // Given
        AddressUK correspondenceAddress = AddressUK.builder()
            .addressLine1("123 Test Street")
            .postTown("Test City")
            .postCode("M1 1AA")
            .build();

        DefendantDetails defendantsDetails = DefendantDetails.builder()
            .addressSameAsPossession(VerticalYesNo.NO)
            .addressKnown(VerticalYesNo.YES)
            .correspondenceAddress(correspondenceAddress)
            .build();

        PCSCase caseData = PCSCase.builder()
            .defendant1(defendantsDetails)
            .build();

        when(addressValidator.validateAddressFields(correspondenceAddress)).thenReturn(List.of());
        when(postcodeValidator.getValidationErrors(correspondenceAddress, "defendant1.correspondenceAddress"))
            .thenReturn(List.of());

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNullOrEmpty();
        assertThat(response.getData()).isEqualTo(caseData);
    }
}
