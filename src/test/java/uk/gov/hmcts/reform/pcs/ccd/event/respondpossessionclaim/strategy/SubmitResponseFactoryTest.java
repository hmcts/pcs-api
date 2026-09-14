package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.strategy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.exception.DraftVersionConflictException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class SubmitResponseFactoryTest {
    private static final long CASE_REFERENCE = 1234567890L;

    private final SubmitResponseFactory submitResponseFactory = new SubmitResponseFactory();

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
        assertThat(submitResponseFactory.validateReviewedDraftVersion(5L, 5L, CASE_REFERENCE)).isEmpty();
    }

    @Test
    void validateReviewedDraftVersion_Mismatch_ReturnsDraftChangedError() {
        Optional<SubmitResponse<State>> result =
            submitResponseFactory.validateReviewedDraftVersion(4L, 5L, CASE_REFERENCE);

        assertThat(result).isPresent();
        assertThat(result.get().getErrors()).containsExactly(DraftVersionConflictException.ERROR_MESSAGE);
    }

    @Test
    void validateReviewedDraftVersion_NoReviewedVersionPosted_IsTolerated() {
        assertThat(submitResponseFactory.validateReviewedDraftVersion(null, 5L, CASE_REFERENCE)).isEmpty();
    }
}
