package uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.document.service.exception.DocAssemblyException;

import static org.assertj.core.api.Assertions.assertThat;

class GenerationDetailsTest {

    @Test
    void unknownFailureStillCapturesTheExceptionClassAndMessage() {
        // The reason enum is coarse ("UNKNOWN"); errorDetail keeps the concrete exception for debugging.
        GenerationDetails details = GenerationDetails.forFailure(
            DocumentType.CLAIM, new NullPointerException("party.getId() is null"), false);

        assertThat(details.failureReason()).isEqualTo(FailureReason.UNKNOWN);
        assertThat(details.errorDetail()).isEqualTo("NullPointerException: party.getId() is null");
        assertThat(details.terminal()).isFalse();
        assertThat(details.documentType()).isEqualTo(DocumentType.CLAIM);
    }

    @Test
    void capturesClassOnlyWhenExceptionHasNoMessage() {
        GenerationDetails details = GenerationDetails.forFailure(
            DocumentType.DEFENDANT_ACCESS_CODE, new NullPointerException(), true);

        assertThat(details.errorDetail()).isEqualTo("NullPointerException");
    }

    @Test
    void capturesDetailAlongsideAClassifiedReason() {
        GenerationDetails details = GenerationDetails.forFailure(
            DocumentType.COUNTERCLAIM, new DocAssemblyException("docmosis returned 500"), false);

        assertThat(details.failureReason()).isEqualTo(FailureReason.RENDER_FAILED);
        assertThat(details.errorDetail()).isEqualTo("DocAssemblyException: docmosis returned 500");
    }

    @Test
    void nullCauseYieldsUnknownReasonAndNoDetail() {
        GenerationDetails details = GenerationDetails.forFailure(DocumentType.CLAIM, null, true);

        assertThat(details.failureReason()).isEqualTo(FailureReason.UNKNOWN);
        assertThat(details.errorDetail()).isNull();
    }

    @Test
    void truncatesAnOverlongMessage() {
        String longMessage = "x".repeat(1000);
        GenerationDetails details = GenerationDetails.forFailure(
            DocumentType.CLAIM, new RuntimeException(longMessage), false);

        assertThat(details.errorDetail()).hasSize(500);
    }

    @Test
    void prefixesTheErrorDetailWithTheContext() {
        GenerationDetails details = GenerationDetails.forFailure(
            DocumentType.DEFENDANT_ACCESS_CODE, new DocAssemblyException("Document generation failed"), false,
            "Welsh letter");

        assertThat(details.errorDetail()).isEqualTo("Welsh letter: DocAssemblyException: Document generation failed");
        assertThat(details.failureReason()).isEqualTo(FailureReason.RENDER_FAILED);
    }

    @Test
    void classifiesFromTheCauseNotTheContext() {
        GenerationDetails details = GenerationDetails.forFailure(
            DocumentType.DEFENDANT_ACCESS_CODE,
            new IllegalStateException(
                "No case management location set for case 1; cannot resolve respond-by-post court"),
            false, "English letter");

        assertThat(details.failureReason()).isEqualTo(FailureReason.NO_COURT_LOCATION);
    }

    @Test
    void truncatesAPrefixedDetailAt500Characters() {
        GenerationDetails details = GenerationDetails.forFailure(
            DocumentType.DEFENDANT_ACCESS_CODE, new RuntimeException("x".repeat(600)), false, "Welsh letter");

        assertThat(details.errorDetail())
            .hasSize(500)
            .startsWith("Welsh letter: RuntimeException: ");
    }

    @Test
    void nullContextGivesTheSameDetailsAsTheThreeArgumentForm() {
        RuntimeException cause = new RuntimeException("docmosis down");

        assertThat(GenerationDetails.forFailure(DocumentType.DEFENDANT_ACCESS_CODE, cause, true, null))
            .isEqualTo(GenerationDetails.forFailure(DocumentType.DEFENDANT_ACCESS_CODE, cause, true));
    }
}
