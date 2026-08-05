package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.manageparty;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.UpdatePartyDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.AddressValidator;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;
import uk.gov.hmcts.reform.pcs.ccd.util.PostcodeValidator;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService.CHARACTER_LIMIT_ERROR_TEMPLATE;
import static uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService.EXTRA_SHORT_TEXT_LIMIT;

@ExtendWith(MockitoExtension.class)
class UpdatePartyDetailsPageTest extends BasePageTest {

    private static final AddressUK VALID_ADDRESS = AddressUK.builder()
        .addressLine1("1 Test Street")
        .postTown("London")
        .postCode("SW1A1AA")
        .build();

    @BeforeEach
    void setUp() {
        AddressValidator addressValidator = new AddressValidator(new PostcodeValidator());
        TextAreaValidationService textAreaValidationService = new TextAreaValidationService();
        setPageUnderTest(new UpdatePartyDetailsPage(addressValidator, textAreaValidationService));
    }

    @Test
    void shouldAcceptValidAddress() {
        // Given
        AddressUK validAddress = AddressUK.builder()
            .addressLine1("1 Test Street")
            .postTown("London")
            .postCode("SW1A1AA")
            .build();

        UpdatePartyDetails updatePartyDetails = UpdatePartyDetails.builder().address(validAddress).build();
        PCSCase caseData = PCSCase.builder().updatePartyDetails(updatePartyDetails).build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrorMessageOverride()).isNullOrEmpty();
    }

    @Test
    void shouldRejectAddressMissingTownAndPostcode() {
        // Given
        AddressUK invalidAddress = AddressUK.builder().addressLine1("1 Test Street").build();

        UpdatePartyDetails updatePartyDetails = UpdatePartyDetails.builder().address(invalidAddress).build();
        PCSCase caseData = PCSCase.builder().updatePartyDetails(updatePartyDetails).build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrorMessageOverride()).contains("Town or City is required");
        assertThat(response.getErrorMessageOverride()).contains("Postcode is required");
    }

    @Test
    void shouldRejectEmailOverCharacterLimit() {
        // Given
        String tooLong = "a".repeat(EXTRA_SHORT_TEXT_LIMIT + 1);
        UpdatePartyDetails updatePartyDetails = UpdatePartyDetails.builder()
            .address(VALID_ADDRESS)
            .email(tooLong)
            .build();
        PCSCase caseData = PCSCase.builder().updatePartyDetails(updatePartyDetails).build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        String expectedError = String.format(CHARACTER_LIMIT_ERROR_TEMPLATE, "Email address", "60");
        assertThat(response.getErrorMessageOverride()).isEqualTo(expectedError);
    }
}