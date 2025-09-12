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
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.util.PostcodeValidator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefendantsDetailsTest extends BasePageTest {

    private PostcodeValidator postcodeValidator;
    private Event<PCSCase, UserRole, State> event;

    @BeforeEach
    void setUp() {
        postcodeValidator = mock(PostcodeValidator.class);
        event = buildPageInTestEvent(new DefendantsDetails(postcodeValidator));
    }

    @Test
    void shouldPassValidationWhenNoDefendant1() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .defendant1(null)
            .build();

        CaseDetails<PCSCase, State> caseDetails = CaseDetails.<PCSCase, State>builder()
            .data(caseData)
            .build();

        // When
        MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "defendantsDetails");
        AboutToStartOrSubmitResponse<PCSCase, State> response = midEvent.handle(caseDetails, null);

        // Then
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData()).isEqualTo(caseData);
    }

    @Test
    void shouldPassValidationWhenDefendant1HasNoCorrespondenceAddress() {
        // Given
        DefendantDetails defendant1 = DefendantDetails.builder()
            .correspondenceAddress(null)
            .build();

        PCSCase caseData = PCSCase.builder()
            .defendant1(defendant1)
            .build();

        CaseDetails<PCSCase, State> caseDetails = CaseDetails.<PCSCase, State>builder()
            .data(caseData)
            .build();

        // When
        MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "defendantsDetails");
        AboutToStartOrSubmitResponse<PCSCase, State> response = midEvent.handle(caseDetails, null);

        // Then
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData()).isEqualTo(caseData);
    }

    @Test
    void shouldPassValidationWhenPostcodeIsValid() {
        // Given
        AddressUK correspondenceAddress = AddressUK.builder()
            .postCode("M1 1AA")
            .build();

        DefendantDetails defendant1 = DefendantDetails.builder()
            .correspondenceAddress(correspondenceAddress)
            .build();

        PCSCase caseData = PCSCase.builder()
            .defendant1(defendant1)
            .build();

        CaseDetails<PCSCase, State> caseDetails = CaseDetails.<PCSCase, State>builder()
            .data(caseData)
            .build();

        when(postcodeValidator.getValidationErrors(any(AddressUK.class), anyString()))
            .thenReturn(java.util.Collections.emptyList());

        // When
        MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "defendantsDetails");
        AboutToStartOrSubmitResponse<PCSCase, State> response = midEvent.handle(caseDetails, null);

        // Then
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData()).isEqualTo(caseData);
    }

    @Test
    void shouldReturnErrorWhenPostcodeIsInvalid() {
        // Given
        AddressUK correspondenceAddress = AddressUK.builder()
            .postCode("12345") // Invalid: doesn't start with letter
            .build();

        DefendantDetails defendant1 = DefendantDetails.builder()
            .correspondenceAddress(correspondenceAddress)
            .build();

        PCSCase caseData = PCSCase.builder()
            .defendant1(defendant1)
            .build();

        CaseDetails<PCSCase, State> caseDetails = CaseDetails.<PCSCase, State>builder()
            .data(caseData)
            .build();

        when(postcodeValidator.getValidationErrors(any(AddressUK.class), anyString()))
            .thenReturn(java.util.List.of("Enter a valid postcode for defendant1.correspondenceAddress"));

        // When
        MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "defendantsDetails");
        AboutToStartOrSubmitResponse<PCSCase, State> response = midEvent.handle(caseDetails, null);

        // Then
        assertThat(response.getErrors()).containsExactly("Enter a valid postcode for defendant1.correspondenceAddress");
        assertThat(response.getData()).isEqualTo(caseData);
    }

    @ParameterizedTest
    @ValueSource(strings = {"M1AA", "M1A", "M1A1A1A1", "1MAA", "M", ""})
    void shouldReturnErrorForInvalidPostcodeFormats(String invalidPostcode) {
        // Given
        AddressUK correspondenceAddress = AddressUK.builder()
            .postCode(invalidPostcode)
            .build();

        DefendantDetails defendant1 = DefendantDetails.builder()
            .correspondenceAddress(correspondenceAddress)
            .build();

        PCSCase caseData = PCSCase.builder()
            .defendant1(defendant1)
            .build();

        CaseDetails<PCSCase, State> caseDetails = CaseDetails.<PCSCase, State>builder()
            .data(caseData)
            .build();

        when(postcodeValidator.getValidationErrors(any(AddressUK.class), anyString()))
            .thenReturn(java.util.List.of("Enter a valid postcode for defendant1.correspondenceAddress"));

        // When
        MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "defendantsDetails");
        AboutToStartOrSubmitResponse<PCSCase, State> response = midEvent.handle(caseDetails, null);

        // Then
        assertThat(response.getErrors()).containsExactly("Enter a valid postcode for defendant1.correspondenceAddress");
    }

    @ParameterizedTest
    @ValueSource(strings = {"M1 1AA", "SW1A 1AA", "B33 8TH", "W1A 0AX", "M123456"})
    void shouldPassValidationForValidPostcodeFormats(String validPostcode) {
        // Given
        AddressUK correspondenceAddress = AddressUK.builder()
            .postCode(validPostcode)
            .build();

        DefendantDetails defendant1 = DefendantDetails.builder()
            .correspondenceAddress(correspondenceAddress)
            .build();

        PCSCase caseData = PCSCase.builder()
            .defendant1(defendant1)
            .build();

        CaseDetails<PCSCase, State> caseDetails = CaseDetails.<PCSCase, State>builder()
            .data(caseData)
            .build();

        when(postcodeValidator.getValidationErrors(any(AddressUK.class), anyString()))
            .thenReturn(java.util.Collections.emptyList());

        // When
        MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "defendantsDetails");
        AboutToStartOrSubmitResponse<PCSCase, State> response = midEvent.handle(caseDetails, null);

        // Then
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData()).isEqualTo(caseData);
    }
}
