package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.util.PostcodeValidator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CaseworkerUpdateApplicationTest {

    private PcsCaseService pcsCaseService;
    private PostcodeValidator postcodeValidator;
    private CaseworkerUpdateApplication caseworkerUpdateApplication;

    @BeforeEach
    void setUp() {
        pcsCaseService = mock(PcsCaseService.class);
        postcodeValidator = mock(PostcodeValidator.class);
        caseworkerUpdateApplication = new CaseworkerUpdateApplication(pcsCaseService, postcodeValidator);
    }

    @Test
    void shouldPassValidationWhenNoPropertyAddress() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .propertyAddress(null)
            .build();

        CaseDetails<PCSCase, State> caseDetails = CaseDetails.<PCSCase, State>builder()
            .data(caseData)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = caseworkerUpdateApplication
            .validatePostcode(caseDetails, null);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData()).isEqualTo(caseData);
    }

    @Test
    void shouldPassValidationWhenPropertyAddressHasNoPostcode() {
        // Given
        AddressUK propertyAddress = AddressUK.builder()
            .postCode(null)
            .build();

        PCSCase caseData = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .build();

        CaseDetails<PCSCase, State> caseDetails = CaseDetails.<PCSCase, State>builder()
            .data(caseData)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = caseworkerUpdateApplication
            .validatePostcode(caseDetails, null);

        // Then
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData()).isEqualTo(caseData);
    }

    @Test
    void shouldPassValidationWhenPostcodeIsValid() {
        // Given
        AddressUK propertyAddress = AddressUK.builder()
            .postCode("M1 1AA")
            .build();

        PCSCase caseData = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .build();

        CaseDetails<PCSCase, State> caseDetails = CaseDetails.<PCSCase, State>builder()
            .data(caseData)
            .build();

        when(postcodeValidator.isValidPostcode("M1 1AA")).thenReturn(true);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = caseworkerUpdateApplication
            .validatePostcode(caseDetails, null);

        // Then
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData()).isEqualTo(caseData);
    }

    @Test
    void shouldReturnErrorWhenPostcodeIsInvalid() {
        // Given
        AddressUK propertyAddress = AddressUK.builder()
            .postCode("12345") // Invalid: doesn't start with letter
            .build();

        PCSCase caseData = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .build();

        CaseDetails<PCSCase, State> caseDetails = CaseDetails.<PCSCase, State>builder()
            .data(caseData)
            .build();

        when(postcodeValidator.isValidPostcode("12345")).thenReturn(false);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = caseworkerUpdateApplication
            .validatePostcode(caseDetails, null);

        // Then
        assertThat(response.getErrors()).containsExactly("Enter a valid postcode");
        assertThat(response.getData()).isEqualTo(caseData);
    }

    @ParameterizedTest
    @ValueSource(strings = {"M1AA", "M1A", "M1A1A1A1", "1MAA", "M", ""})
    void shouldReturnErrorForInvalidPostcodeFormats(String invalidPostcode) {
        // Given
        AddressUK propertyAddress = AddressUK.builder()
            .postCode(invalidPostcode)
            .build();

        PCSCase caseData = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .build();

        CaseDetails<PCSCase, State> caseDetails = CaseDetails.<PCSCase, State>builder()
            .data(caseData)
            .build();

        when(postcodeValidator.isValidPostcode(invalidPostcode)).thenReturn(false);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = caseworkerUpdateApplication
            .validatePostcode(caseDetails, null);

        // Then
        assertThat(response.getErrors()).containsExactly("Enter a valid postcode");
    }

    @ParameterizedTest
    @ValueSource(strings = {"M1 1AA", "SW1A 1AA", "B33 8TH", "W1A 0AX", "M123456"})
    void shouldPassValidationForValidPostcodeFormats(String validPostcode) {
        // Given
        AddressUK propertyAddress = AddressUK.builder()
            .postCode(validPostcode)
            .build();

        PCSCase caseData = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .build();

        CaseDetails<PCSCase, State> caseDetails = CaseDetails.<PCSCase, State>builder()
            .data(caseData)
            .build();

        when(postcodeValidator.isValidPostcode(validPostcode)).thenReturn(true);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = caseworkerUpdateApplication
            .validatePostcode(caseDetails, null);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData()).isEqualTo(caseData);
        
        // Verify the mock was called
        verify(postcodeValidator).isValidPostcode(validPostcode);
    }

    @Test
    void shouldCallPcsCaseServiceOnSubmit() {
        // Given
        long caseReference = 12345L;
        PCSCase caseData = PCSCase.builder()
            .propertyAddress(AddressUK.builder().postCode("M1 1AA").build())
            .build();

        @SuppressWarnings("unchecked")
        uk.gov.hmcts.ccd.sdk.api.EventPayload<PCSCase, State> eventPayload = mock(
            uk.gov.hmcts.ccd.sdk.api.EventPayload.class);
        when(eventPayload.caseReference()).thenReturn(caseReference);
        when(eventPayload.caseData()).thenReturn(caseData);

        // When
        caseworkerUpdateApplication.submit(eventPayload);

        // Then
        verify(pcsCaseService).patchCase(caseReference, caseData);
    }

    @Test
    void shouldHandleEmptyPostcodeString() {
        // Given
        AddressUK propertyAddress = AddressUK.builder()
            .postCode("")
            .build();

        PCSCase caseData = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .build();

        CaseDetails<PCSCase, State> caseDetails = CaseDetails.<PCSCase, State>builder()
            .data(caseData)
            .build();

        when(postcodeValidator.isValidPostcode("")).thenReturn(false);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = caseworkerUpdateApplication
            .validatePostcode(caseDetails, null);

        // Then
        assertThat(response.getErrors()).containsExactly("Enter a valid postcode");
    }

    @Test
    void shouldHandleWhitespaceOnlyPostcode() {
        // Given
        AddressUK propertyAddress = AddressUK.builder()
            .postCode("   ")
            .build();

        PCSCase caseData = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .build();

        CaseDetails<PCSCase, State> caseDetails = CaseDetails.<PCSCase, State>builder()
            .data(caseData)
            .build();

        when(postcodeValidator.isValidPostcode("   ")).thenReturn(false);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = caseworkerUpdateApplication
            .validatePostcode(caseDetails, null);

        // Then
        assertThat(response.getErrors()).containsExactly("Enter a valid postcode");
    }
}
