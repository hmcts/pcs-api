package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.api.callback.MidEvent;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.util.PostcodeValidator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContactPreferencesTest extends BasePageTest {

    private PostcodeValidator postcodeValidator;
    private Event<PCSCase, UserRole, State> event;

    @BeforeEach
    void setUp() {
        postcodeValidator = mock(PostcodeValidator.class);
        event = buildPageInTestEvent(new ContactPreferences(postcodeValidator));
    }

    @Test
    void shouldPassValidationWhenNoOverriddenClaimantContactAddress() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .overriddenClaimantContactAddress(null)
            .build();

        CaseDetails<PCSCase, State> caseDetails = CaseDetails.<PCSCase, State>builder()
            .data(caseData)
            .build();

        // When
        MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "contactPreferences");
        AboutToStartOrSubmitResponse<PCSCase, State> response = midEvent.handle(caseDetails, null);

        // Then
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData()).isEqualTo(caseData);
    }

    @Test
    void shouldPassValidationWhenPostcodeIsValid() {
        // Given
        AddressUK overriddenAddress = AddressUK.builder()
            .postCode("M1 1AA")
            .build();

        PCSCase caseData = PCSCase.builder()
            .overriddenClaimantContactAddress(overriddenAddress)
            .build();

        CaseDetails<PCSCase, State> caseDetails = CaseDetails.<PCSCase, State>builder()
            .data(caseData)
            .build();

        when(postcodeValidator.getValidationErrors(any(AddressUK.class), anyString()))
            .thenReturn(java.util.Collections.emptyList());

        // When
        MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "contactPreferences");
        AboutToStartOrSubmitResponse<PCSCase, State> response = midEvent.handle(caseDetails, null);

        // Then
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData()).isEqualTo(caseData);
    }

    @Test
    void shouldReturnErrorWhenPostcodeIsInvalid() {
        // Given
        AddressUK overriddenAddress = AddressUK.builder()
            .postCode("12345") // Invalid: doesn't start with letter
            .build();

        PCSCase caseData = PCSCase.builder()
            .overriddenClaimantContactAddress(overriddenAddress)
            .build();

        CaseDetails<PCSCase, State> caseDetails = CaseDetails.<PCSCase, State>builder()
            .data(caseData)
            .build();

        when(postcodeValidator.getValidationErrors(any(AddressUK.class), anyString()))
            .thenReturn(java.util.List.of("Enter a valid postcode for overriddenClaimantContactAddress"));

        // When
        MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "contactPreferences");
        AboutToStartOrSubmitResponse<PCSCase, State> response = midEvent.handle(caseDetails, null);

        // Then
        assertThat(response.getErrors()).containsExactly("Enter a valid postcode for overriddenClaimantContactAddress");
        assertThat(response.getData()).isEqualTo(caseData);
    }

    @ParameterizedTest
    @ValueSource(strings = {"M1AA", "M1A", "M1A1A1A1", "1MAA", "M", ""})
    void shouldReturnErrorForInvalidPostcodeFormats(String invalidPostcode) {
        // Given
        AddressUK overriddenAddress = AddressUK.builder()
            .postCode(invalidPostcode)
            .build();

        PCSCase caseData = PCSCase.builder()
            .overriddenClaimantContactAddress(overriddenAddress)
            .build();

        CaseDetails<PCSCase, State> caseDetails = CaseDetails.<PCSCase, State>builder()
            .data(caseData)
            .build();

        when(postcodeValidator.getValidationErrors(any(AddressUK.class), anyString()))
            .thenReturn(java.util.List.of("Enter a valid postcode for overriddenClaimantContactAddress"));

        // When
        MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "contactPreferences");
        AboutToStartOrSubmitResponse<PCSCase, State> response = midEvent.handle(caseDetails, null);

        // Then
        assertThat(response.getErrors()).containsExactly("Enter a valid postcode for overriddenClaimantContactAddress");
    }

    @ParameterizedTest
    @ValueSource(strings = {"M1 1AA", "SW1A 1AA", "B33 8TH", "W1A 0AX", "M123456"})
    void shouldPassValidationForValidPostcodeFormats(String validPostcode) {
        // Given
        AddressUK overriddenAddress = AddressUK.builder()
            .postCode(validPostcode)
            .build();

        PCSCase caseData = PCSCase.builder()
            .overriddenClaimantContactAddress(overriddenAddress)
            .build();

        CaseDetails<PCSCase, State> caseDetails = CaseDetails.<PCSCase, State>builder()
            .data(caseData)
            .build();

        when(postcodeValidator.getValidationErrors(any(AddressUK.class), anyString()))
            .thenReturn(java.util.Collections.emptyList());

        // When
        MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "contactPreferences");
        AboutToStartOrSubmitResponse<PCSCase, State> response = midEvent.handle(caseDetails, null);

        // Then
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData()).isEqualTo(caseData);
    }

    @Test
    void shouldHandleMultipleValidationErrors() {
        // Given
        AddressUK overriddenAddress = AddressUK.builder()
            .postCode("12345") // Invalid: doesn't start with letter
            .build();

        PCSCase caseData = PCSCase.builder()
            .overriddenClaimantContactAddress(overriddenAddress)
            .build();

        CaseDetails<PCSCase, State> caseDetails = CaseDetails.<PCSCase, State>builder()
            .data(caseData)
            .build();

        when(postcodeValidator.getValidationErrors(any(AddressUK.class), anyString()))
            .thenReturn(java.util.List.of(
                "Enter a valid postcode for overriddenClaimantContactAddress",
                "Additional validation error"
            ));

        // When
        MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "contactPreferences");
        AboutToStartOrSubmitResponse<PCSCase, State> response = midEvent.handle(caseDetails, null);

        // Then
        assertThat(response.getErrors()).containsExactly(
            "Enter a valid postcode for overriddenClaimantContactAddress",
            "Additional validation error"
        );
        assertThat(response.getData()).isEqualTo(caseData);
    }
}
