package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.strategy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.RespondToClaimCallbackError;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantContactDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.service.AddressValidator;
import uk.gov.hmcts.reform.pcs.ccd.util.PostcodeValidator;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class SubmitResponseFactoryTest {
    private static final long CASE_REFERENCE = 1234567890L;

    private final SubmitResponseFactory submitResponseFactory =
        new SubmitResponseFactory(new AddressValidator(new PostcodeValidator()));

    @Test
    void validate_WithNullPossessionClaimResponse_ReturnsError() {
        // when
        Optional<SubmitResponse<State>> result = submitResponseFactory
            .validate(null, CASE_REFERENCE);

        // then
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get().getErrors()).contains("Invalid submission: missing response data");
    }

    @Test
    void validate_WithNullDefendantResponses_ReturnsError() {
        // given
        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder().build();

        // when
        Optional<SubmitResponse<State>> result = submitResponseFactory
            .validate(possessionClaimResponse, CASE_REFERENCE);

        // then
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get().getErrors()).contains("Invalid submission: missing defendant response data");
    }

    @Test
    void validate_WithDefendantResponses_ReturnsNull() {
        // given
        DefendantResponses defendantResponses = DefendantResponses.builder().build();
        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(defendantResponses)
            .build();

        // when
        Optional<SubmitResponse<State>> result = submitResponseFactory
            .validate(possessionClaimResponse, CASE_REFERENCE);

        // then
        assertThat(result).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
        "W5, Enter a valid postcode for correspondence address",
        "12345, Enter a valid postcode for correspondence address",
        "' ', Postcode is required for correspondence address"
    })
    void validate_WithInvalidEnteredCorrespondenceAddress_ReturnsError(String postcode, String expectedError) {
        // given
        PossessionClaimResponse possessionClaimResponse = responseWithEnteredAddress(postcode, VerticalYesNo.NO);

        // when
        Optional<SubmitResponse<State>> result = submitResponseFactory
            .validate(possessionClaimResponse, CASE_REFERENCE);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getErrors()).containsExactly(expectedError);
    }

    @Test
    void validate_WithValidEnteredCorrespondenceAddress_ReturnsEmpty() {
        // given
        PossessionClaimResponse possessionClaimResponse = responseWithEnteredAddress("W3 7RX", VerticalYesNo.NO);

        // when
        Optional<SubmitResponse<State>> result = submitResponseFactory
            .validate(possessionClaimResponse, CASE_REFERENCE);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void validate_WithInvalidEnteredAddressOnPropertyAddressFallback_ReturnsError() {
        // given - the claimant gave no address, so the defendant answered No to the property address instead
        AddressUK address = AddressUK.builder()
            .addressLine1("1 Second Avenue")
            .postTown("London")
            .postCode("W5")
            .build();
        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(DefendantResponses.builder()
                                    .propertyAddressConfirmation(VerticalYesNo.NO)
                                    .build())
            .defendantContactDetails(DefendantContactDetails.builder()
                                         .party(Party.builder().address(address).build())
                                         .build())
            .build();

        // when
        Optional<SubmitResponse<State>> result = submitResponseFactory
            .validate(possessionClaimResponse, CASE_REFERENCE);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getErrors()).containsExactly("Enter a valid postcode for correspondence address");
    }

    @Test
    void validate_DoesNotValidateAddressWhenClaimantAddressConfirmed() {
        // given - the address on the draft is the claimant's, not one the defendant typed in
        PossessionClaimResponse possessionClaimResponse = responseWithEnteredAddress("W5", VerticalYesNo.YES);

        // when
        Optional<SubmitResponse<State>> result = submitResponseFactory
            .validate(possessionClaimResponse, CASE_REFERENCE);

        // then
        assertThat(result).isEmpty();
    }

    private static PossessionClaimResponse responseWithEnteredAddress(String postcode, VerticalYesNo confirmation) {
        AddressUK address = AddressUK.builder()
            .addressLine1("1 Second Avenue")
            .postTown("London")
            .postCode(postcode)
            .build();
        return PossessionClaimResponse.builder()
            .defendantResponses(DefendantResponses.builder()
                                    .correspondenceAddressConfirmation(confirmation)
                                    .build())
            .defendantContactDetails(DefendantContactDetails.builder()
                                         .party(Party.builder().address(address).build())
                                         .build())
            .build();
    }

    @Test
    void success_ReturnsDefaultResponse() {
        // when
        SubmitResponse<State> result = submitResponseFactory.success();

        // then
        assertEquals(SubmitResponse.defaultResponse(), result);
    }

    @Test
    void error_ReturnsResponse() {
        // given
        String error = "error";

        // when
        SubmitResponse<State> result = submitResponseFactory.error(error);

        // then
        assertEquals(error, result.getErrors().getFirst());
    }


    // ----- HDPI-8866 W05 -----

    @Test
    void validateReviewedDraftVersion_Matching_ReturnsEmpty() {
        assertThat(submitResponseFactory.validateReviewedDraftVersion(payloadReviewing(5L), storedDraftAt(5L)))
            .isEmpty();
    }

    @Test
    void validateReviewedDraftVersion_Mismatch_ReturnsDraftChangedError() {
        Optional<SubmitResponse<State>> result =
            submitResponseFactory.validateReviewedDraftVersion(payloadReviewing(4L), storedDraftAt(5L));

        assertThat(result).isPresent();
        assertThat(result.get().getErrors()).containsExactly(RespondToClaimCallbackError.DRAFT_CHANGED);
    }

    @Test
    void validateReviewedDraftVersion_NoReviewedVersionPosted_ReturnsDraftChangedError() {
        Optional<SubmitResponse<State>> result =
            submitResponseFactory.validateReviewedDraftVersion(payloadReviewing(null), storedDraftAt(5L));

        assertThat(result).isPresent();
        assertThat(result.get().getErrors()).containsExactly(RespondToClaimCallbackError.DRAFT_CHANGED);
    }

    private static EventPayload<PCSCase, State> payloadReviewing(Long draftVersion) {
        PCSCase posted = PCSCase.builder()
            .possessionClaimResponse(PossessionClaimResponse.builder().draftVersion(draftVersion).build())
            .build();
        return new EventPayload<>(CASE_REFERENCE, posted, null);
    }

    private static PossessionClaimResponse storedDraftAt(Long draftVersion) {
        return PossessionClaimResponse.builder().draftVersion(draftVersion).build();
    }
}
