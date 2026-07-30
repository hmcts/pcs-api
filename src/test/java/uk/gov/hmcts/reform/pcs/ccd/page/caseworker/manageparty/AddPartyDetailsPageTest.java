package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.manageparty;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.AddPartyDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.PartyType;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.AddressValidator;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;
import uk.gov.hmcts.reform.pcs.ccd.util.PostcodeValidator;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService.CHARACTER_LIMIT_ERROR_TEMPLATE;
import static uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService.EXTRA_SHORT_TEXT_LIMIT;

@ExtendWith(MockitoExtension.class)
class AddPartyDetailsPageTest extends BasePageTest {

    private static final AddressUK VALID_ADDRESS = AddressUK.builder()
        .addressLine1("1 Test Street")
        .postTown("London")
        .postCode("SW1A1AA")
        .build();

    @BeforeEach
    void setUp() {
        TextAreaValidationService textAreaValidationService = new TextAreaValidationService();
        AddressValidator addressValidator = new AddressValidator(new PostcodeValidator());
        setPageUnderTest(new AddPartyDetailsPage(textAreaValidationService, addressValidator));
    }

    @Test
    void shouldAcceptFieldsWithinLimit() {
        // Given
        AddPartyDetails addPartyDetails = AddPartyDetails.builder()
            .addPartyType(PartyType.CLAIMANT)
            .claimantOrganisationName("Acme Ltd")
            .claimantEmail("jane@test.com")
            .claimantPhoneNumber("07000000000")
            .defendantEmail("john@test.com")
            .defendantPhoneNumber("07000000001")
            .litigationFriendOrganisationName("Litigation Friends Ltd")
            .litigationFriendEmail("bob@test.com")
            .litigationFriendPhoneNumber("07000000002")
            .claimantAddress(VALID_ADDRESS)
            .build();
        PCSCase caseData = PCSCase.builder().addPartyDetails(addPartyDetails).build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrorMessageOverride()).isNullOrEmpty();
        assertThat(response.getData()).isEqualTo(caseData);
    }

    @ParameterizedTest
    @MethodSource("rejectScenarios")
    void shouldRejectFieldOverLimit(AddPartyDetails addPartyDetails, String expectedLabel) {
        // Given
        PCSCase caseData = PCSCase.builder().addPartyDetails(addPartyDetails).build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        String expectedError = String.format(CHARACTER_LIMIT_ERROR_TEMPLATE, expectedLabel, "60");
        assertThat(response.getErrorMessageOverride()).isEqualTo(expectedError);
    }

    private static Stream<Arguments> rejectScenarios() {
        String tooLong = "a".repeat(EXTRA_SHORT_TEXT_LIMIT + 1);

        return Stream.of(
            Arguments.of(
                AddPartyDetails.builder()
                    .addPartyType(PartyType.CLAIMANT)
                    .claimantOrganisationName(tooLong)
                    .claimantAddress(VALID_ADDRESS)
                    .build(),
                "Organisation name"),
            Arguments.of(
                AddPartyDetails.builder()
                    .addPartyType(PartyType.CLAIMANT)
                    .claimantEmail(tooLong)
                    .claimantAddress(VALID_ADDRESS)
                    .build(),
                "Email address"),
            Arguments.of(
                AddPartyDetails.builder()
                    .addPartyType(PartyType.CLAIMANT)
                    .claimantPhoneNumber(tooLong)
                    .claimantAddress(VALID_ADDRESS)
                    .build(),
                "Phone number"),
            Arguments.of(
                AddPartyDetails.builder()
                    .addPartyType(PartyType.DEFENDANT)
                    .defendantEmail(tooLong)
                    .defendantAddress(VALID_ADDRESS)
                    .build(),
                "Email address"),
            Arguments.of(
                AddPartyDetails.builder()
                    .addPartyType(PartyType.DEFENDANT)
                    .defendantPhoneNumber(tooLong)
                    .defendantAddress(VALID_ADDRESS)
                    .build(),
                "Phone number"),
            Arguments.of(
                AddPartyDetails.builder()
                    .addPartyType(PartyType.LITIGATION_FRIEND)
                    .litigationFriendOrganisationName(tooLong)
                    .litigationFriendAddress(VALID_ADDRESS)
                    .build(),
                "Organisation name"),
            Arguments.of(
                AddPartyDetails.builder()
                    .addPartyType(PartyType.LITIGATION_FRIEND)
                    .litigationFriendEmail(tooLong)
                    .litigationFriendAddress(VALID_ADDRESS)
                    .build(),
                "Email address"),
            Arguments.of(
                AddPartyDetails.builder()
                    .addPartyType(PartyType.LITIGATION_FRIEND)
                    .litigationFriendPhoneNumber(tooLong)
                    .litigationFriendAddress(VALID_ADDRESS)
                    .build(),
                "Phone number")
        );
    }

    @ParameterizedTest
    @MethodSource("addressScenarios")
    void shouldValidateAddressForSelectedPartyType(PartyType partyType, AddPartyDetails addPartyDetails) {
        // Given
        addPartyDetails.setAddPartyType(partyType);
        PCSCase caseData = PCSCase.builder().addPartyDetails(addPartyDetails).build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrorMessageOverride()).contains("Town or City is required");
        assertThat(response.getErrorMessageOverride()).contains("Postcode is required");
    }

    private static Stream<Arguments> addressScenarios() {
        AddressUK addressMissingTownAndPostcode = AddressUK.builder().addressLine1("1 Test Street").build();

        return Stream.of(
            Arguments.of(PartyType.CLAIMANT,
                AddPartyDetails.builder().claimantAddress(addressMissingTownAndPostcode).build()),
            Arguments.of(PartyType.DEFENDANT,
                AddPartyDetails.builder().defendantAddress(addressMissingTownAndPostcode).build()),
            Arguments.of(PartyType.LITIGATION_FRIEND,
                AddPartyDetails.builder().litigationFriendAddress(addressMissingTownAndPostcode).build())
        );
    }
}
