package uk.gov.hmcts.reform.pcs.ccd.page.legalrepresentativedetails;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.LegalRepresentativeDetails;
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
class LegalRepresentativeContactDetailsPageTest extends BasePageTest {

    @Mock
    private AddressValidator addressValidator;

    @BeforeEach
    void setUp() {
        TextAreaValidationService textAreaValidationService = new TextAreaValidationService();
        setPageUnderTest(new LegalRepresentativeContactDetailsPage(addressValidator, textAreaValidationService));
    }

    @Test
    void shouldReturnValidationErrorsWhenAddressInvalid() {
        // Given
        AddressUK contactAddress = mock(AddressUK.class);
        LegalRepresentativeDetails legalRepresentativeDetails = LegalRepresentativeDetails.builder()
            .differentPostalAddress(VerticalYesNo.YES)
            .updatedCorrespondenceAddress(contactAddress)
            .build();
        PCSCase caseData = PCSCase.builder()
            .legalRepresentativeDetails(legalRepresentativeDetails)
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
        LegalRepresentativeDetails legalRepresentativeDetails = LegalRepresentativeDetails.builder()
            .organisationAddressFound(YesOrNo.NO)
            .useEmailAddress(null)
            .build();

        PCSCase caseData = PCSCase.builder()
            .legalRepresentativeDetails(legalRepresentativeDetails)
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
    void shouldNotValidateOverrideAddressWhenOrganisationAddressFoundAndUserSaysAddressCorrect() {
        // Given
        LegalRepresentativeDetails legalRepresentativeDetails = LegalRepresentativeDetails.builder()
            .organisationAddressFound(YesOrNo.YES)
            .useEmailAddress(VerticalYesNo.YES)
            .emailAddress(null)
            .build();

        PCSCase caseData = PCSCase.builder()
            .legalRepresentativeDetails(legalRepresentativeDetails)
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
        PCSCase caseData = PCSCase.builder().legalRepresentativeDetails(null).build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNull();
        verifyNoInteractions(addressValidator);
    }

    @Test
    void shouldValidateEmailWhenCorrectLength() {
        //Given
        LegalRepresentativeDetails legalRepresentativeDetails = LegalRepresentativeDetails.builder()
            .organisationAddressFound(YesOrNo.YES)
            .useEmailAddress(VerticalYesNo.YES)
            .useEmailAddress(VerticalYesNo.NO)
            .emailAddress("John.Smith@hotmail.com")
            .build();

        PCSCase caseData = PCSCase.builder()
            .legalRepresentativeDetails(legalRepresentativeDetails)
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
        LegalRepresentativeDetails legalRepresentativeDetails = LegalRepresentativeDetails.builder()
            .organisationAddressFound(YesOrNo.YES)
            .differentPostalAddress(VerticalYesNo.NO)
            .useEmailAddress(VerticalYesNo.NO)
            .emailAddress(longEmail)
            .build();

        PCSCase caseData = PCSCase.builder()
            .legalRepresentativeDetails(legalRepresentativeDetails)
            .build();

        //When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        //Then
        assertThat(response.getErrorMessageOverride())
            .contains("more than the maximum number of characters");

    }

}
