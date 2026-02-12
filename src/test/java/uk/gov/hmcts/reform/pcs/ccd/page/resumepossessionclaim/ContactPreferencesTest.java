package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantContactPreferences;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.AddressValidator;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactPreferencesTest extends BasePageTest {

    @Mock
    private AddressValidator addressValidator;

    @BeforeEach
    void setUp() {
        TextAreaValidationService textAreaValidationService = new TextAreaValidationService();
        setPageUnderTest(new ContactPreferences(addressValidator,textAreaValidationService));
    }

    @Test
    void shouldReturnValidationErrorsWhenAddressInvalid() {
        // Given
        AddressUK contactAddress = mock(AddressUK.class);
        ClaimantContactPreferences contactPreferences = ClaimantContactPreferences.builder()
            .isCorrectClaimantContactAddress(VerticalYesNo.NO)
            .overriddenClaimantContactAddress(contactAddress)
            .build();
        PCSCase caseData = PCSCase.builder()
            .claimantContactPreferences(contactPreferences)
            .build();

        List<String> expectedValidationErrors = List.of("error 1", "error 2");
        when(addressValidator.validateAddressFields(contactAddress)).thenReturn(expectedValidationErrors);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrorMessageOverride()).isEqualTo("error 1\nerror 2");
    }

    @Test
    void shouldReturnValidationErrorsWhenOrgAddressNotFoundAndOverrideAddressInvalid() {
        // Given
        ClaimantContactPreferences contactPreferences = ClaimantContactPreferences.builder()
            .orgAddressFound(YesOrNo.NO)
            .overriddenClaimantContactAddress(null)
            .build();

        PCSCase caseData = PCSCase.builder()
            .claimantContactPreferences(contactPreferences)
            .build();

        String expectedError = "addressLine1 missing";
        when(addressValidator.validateAddressFields(null)).thenReturn(List.of(expectedError));

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrorMessageOverride()).isEqualTo(expectedError);
        verify(addressValidator).validateAddressFields(null);
    }

    @Test
    void shouldNotValidateOverrideAddressWhenOrgAddressFoundAndUserSaysAddressCorrect() {
        // Given
        ClaimantContactPreferences contactPreferences = ClaimantContactPreferences.builder()
            .orgAddressFound(YesOrNo.YES)
            .isCorrectClaimantContactAddress(VerticalYesNo.YES)
            .overriddenClaimantContactAddress(null)
            .build();

        PCSCase caseData = PCSCase.builder()
            .claimantContactPreferences(contactPreferences)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNull();
        verifyNoInteractions(addressValidator);
    }

    @Test
    void shouldNotErrorWhenContactPreferencesNull() {
        // Given
        PCSCase caseData = PCSCase.builder().claimantContactPreferences(null).build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNull();
        verifyNoInteractions(addressValidator);
    }

    @Test
    void shouldValidateEmailWhenCorrectLength() {
        //Given
        ClaimantContactPreferences contactPreferences = ClaimantContactPreferences.builder()
            .orgAddressFound(YesOrNo.YES)
            .isCorrectClaimantContactAddress(VerticalYesNo.YES)
            .overriddenClaimantContactAddress(null)
            .isCorrectClaimantContactEmail(VerticalYesNo.NO)
            .overriddenClaimantContactEmail("John.Smith@hotmail.com")
            .build();

        PCSCase caseData = PCSCase.builder()
            .claimantContactPreferences(contactPreferences)
            .build();

        //When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        //Then
        assertThat(response.getErrors()).isNull();
    }

    @Test
    void shouldValidateEmailWhenTooLong() {
        //Given
        String longEmail = "John.Smith@hotmail.com".repeat(4);
        ClaimantContactPreferences contactPreferences = ClaimantContactPreferences.builder()
            .orgAddressFound(YesOrNo.YES)
            .isCorrectClaimantContactAddress(VerticalYesNo.YES)
            .overriddenClaimantContactAddress(null)
            .isCorrectClaimantContactEmail(VerticalYesNo.NO)
            .overriddenClaimantContactEmail(longEmail)
            .build();

        PCSCase caseData = PCSCase.builder()
            .claimantContactPreferences(contactPreferences)
            .build();

        //When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        //Then
        assertThat(response.getErrorMessageOverride())
            .contains("more than the maximum number of characters");

    }
}
