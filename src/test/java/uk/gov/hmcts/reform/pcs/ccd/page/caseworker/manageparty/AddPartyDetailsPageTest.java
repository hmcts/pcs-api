package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.manageparty;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.AddPartyDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService.CHARACTER_LIMIT_ERROR_TEMPLATE;
import static uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService.EXTRA_SHORT_TEXT_LIMIT;

@ExtendWith(MockitoExtension.class)
class AddPartyDetailsPageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        TextAreaValidationService textAreaValidationService = new TextAreaValidationService();
        setPageUnderTest(new AddPartyDetailsPage(textAreaValidationService));
    }

    @Test
    void shouldAcceptFieldsWithinLimit() {
        // Given
        AddPartyDetails addPartyDetails = AddPartyDetails.builder()
            .claimantOrganisationName("Acme Ltd")
            .claimantEmail("jane@test.com")
            .claimantPhoneNumber("07000000000")
            .defendantEmail("john@test.com")
            .defendantPhoneNumber("07000000001")
            .litigationFriendOrganisationName("Litigation Friends Ltd")
            .litigationFriendEmail("bob@test.com")
            .litigationFriendPhoneNumber("07000000002")
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
                AddPartyDetails.builder().claimantOrganisationName(tooLong).build(), "Organisation name"),
            Arguments.of(AddPartyDetails.builder().claimantEmail(tooLong).build(), "Email address"),
            Arguments.of(AddPartyDetails.builder().claimantPhoneNumber(tooLong).build(), "Phone number"),
            Arguments.of(AddPartyDetails.builder().defendantEmail(tooLong).build(), "Email address"),
            Arguments.of(AddPartyDetails.builder().defendantPhoneNumber(tooLong).build(), "Phone number"),
            Arguments.of(
                AddPartyDetails.builder().litigationFriendOrganisationName(tooLong).build(), "Organisation name"),
            Arguments.of(AddPartyDetails.builder().litigationFriendEmail(tooLong).build(), "Email address"),
            Arguments.of(AddPartyDetails.builder().litigationFriendPhoneNumber(tooLong).build(), "Phone number")
        );
    }
}
