package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.manageparty;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.PartyType;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.UpdatePartyDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.AddressValidator;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;
import uk.gov.hmcts.reform.pcs.ccd.util.PostcodeValidator;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService.CHARACTER_LIMIT_ERROR_TEMPLATE;
import static uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService.EXTRA_SHORT_TEXT_LIMIT;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

@ExtendWith(MockitoExtension.class)
class UpdatePartyDetailsPageTest extends BasePageTest {

    private static final AddressUK VALID_ADDRESS = AddressUK.builder()
        .addressLine1("1 Test Street")
        .postTown("London")
        .postCode("SW1A1AA")
        .build();

    private static final LocalDate FIXED_CURRENT_DATE = LocalDate.of(2025, 8, 27);

    @Mock
    private Clock ukClock;

    @BeforeEach
    void setUp() {
        lenient().when(ukClock.instant()).thenReturn(FIXED_CURRENT_DATE.atTime(10, 20).atZone(UK_ZONE_ID).toInstant());
        lenient().when(ukClock.getZone()).thenReturn(UK_ZONE_ID);

        AddressValidator addressValidator = new AddressValidator(new PostcodeValidator());
        TextAreaValidationService textAreaValidationService = new TextAreaValidationService();
        setPageUnderTest(new UpdatePartyDetailsPage(addressValidator, textAreaValidationService, ukClock));
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
    void shouldAcceptDateOfBirthInThePast() {
        // Given
        UpdatePartyDetails updatePartyDetails = UpdatePartyDetails.builder()
            .partyType(PartyType.DEFENDANT)
            .address(VALID_ADDRESS)
            .dateOfBirth(Optional.of(FIXED_CURRENT_DATE.minusDays(1)))
            .build();
        PCSCase caseData = PCSCase.builder().updatePartyDetails(updatePartyDetails).build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrorMessageOverride()).isNullOrEmpty();
    }

    @Test
    void shouldRejectDateOfBirthNotInThePast() {
        // Given
        UpdatePartyDetails updatePartyDetails = UpdatePartyDetails.builder()
            .partyType(PartyType.DEFENDANT)
            .address(VALID_ADDRESS)
            .dateOfBirth(Optional.of(FIXED_CURRENT_DATE))
            .build();
        PCSCase caseData = PCSCase.builder().updatePartyDetails(updatePartyDetails).build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrorMessageOverride()).isEqualTo("Date of birth must be in the past");
    }

    @Test
    void shouldNotValidateDateOfBirthForClaimant() {
        // Given
        UpdatePartyDetails updatePartyDetails = UpdatePartyDetails.builder()
            .partyType(PartyType.CLAIMANT)
            .address(VALID_ADDRESS)
            .dateOfBirth(Optional.of(FIXED_CURRENT_DATE))
            .build();
        PCSCase caseData = PCSCase.builder().updatePartyDetails(updatePartyDetails).build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrorMessageOverride()).isNullOrEmpty();
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