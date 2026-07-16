package uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog;

import feign.FeignException;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.BulkPrintMergeException;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.MissingPostalAddressException;
import uk.gov.hmcts.reform.pcs.document.service.exception.DocAssemblyException;
import uk.gov.hmcts.reform.pcs.document.service.exception.DocumentStoreException;
import uk.gov.hmcts.reform.pcs.exception.DocumentDownloadException;
import uk.gov.hmcts.reform.pcs.exception.ErrorCode;
import uk.gov.hmcts.reform.pcs.exception.RedactionContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static uk.gov.hmcts.reform.pcs.exception.ErrorCode.BULK_PRINT_MERGE_ERROR;
import static uk.gov.hmcts.reform.pcs.exception.ErrorCode.META_DATA_FOR_DOCUMENT_ERROR;
import static uk.gov.hmcts.reform.pcs.exception.ErrorCode.MISSING_POSTAL_ADDRESS;

class FailureReasonsTest {

    @Test
    void mapsMissingAddress() {
        assertThat(FailureReasons.from(new MissingPostalAddressException(MISSING_POSTAL_ADDRESS,
                                                                         RedactionContext.empty())))
            .isEqualTo(FailureReason.MISSING_ADDRESS);
    }

    @Test
    void mapsRenderFailed() {
        assertThat(FailureReasons.from(new DocAssemblyException(ErrorCode.DOC_GENERATION_UNEXPECTED_ERROR)))
            .isEqualTo(FailureReason.RENDER_FAILED);
    }

    @Test
    void mapsMergeFailed() {
        assertThat(FailureReasons.from(new BulkPrintMergeException(BULK_PRINT_MERGE_ERROR, new RuntimeException())))
            .isEqualTo(FailureReason.MERGE_FAILED);
    }

    @Test
    void mapsDocumentFetchFailed() {
        assertThat(FailureReasons.from(new DocumentDownloadException("fetch", new RuntimeException())))
            .isEqualTo(FailureReason.DOCUMENT_FETCH_FAILED);
    }

    @Test
    void mapsDocumentStoreFailed() {
        RedactionContext redactionContext = RedactionContext.builder()
            .value("cdam down", "cdam down").build();
        assertThat(FailureReasons.from(new DocumentStoreException(META_DATA_FOR_DOCUMENT_ERROR,
                                                                  redactionContext, new RuntimeException())))
            .isEqualTo(FailureReason.DOCUMENT_STORE_FAILED);
    }

    @Test
    void storeFailureWrappingFeignIsClassifiedAsStoreNotSend() {
        // The store failure wraps a FeignException from CDAM; it must NOT fall through to the
        // FeignException -> SEND_LETTER_UNAVAILABLE catch-all.
        RedactionContext redactionContext = RedactionContext.builder()
            .value("cdam down", "cdam down").build();
        DocumentStoreException storeFailure = new DocumentStoreException(META_DATA_FOR_DOCUMENT_ERROR,
                                                                         redactionContext, mock(FeignException.class));
        assertThat(FailureReasons.from(storeFailure)).isEqualTo(FailureReason.DOCUMENT_STORE_FAILED);
    }

    @Test
    void mapsSendLetterUnavailableForFeign() {
        assertThat(FailureReasons.from(mock(FeignException.class)))
            .isEqualTo(FailureReason.SEND_LETTER_UNAVAILABLE);
    }

    @Test
    void mapsNoCourtLocationFromCaseManagementMessage() {
        assertThat(FailureReasons.from(new IllegalStateException("No case management location set for case 123")))
            .isEqualTo(FailureReason.NO_COURT_LOCATION);
    }

    @Test
    void mapsNoCourtLocationFromRespondByPostCourtMessage() {
        assertThat(FailureReasons.from(new IllegalStateException("cannot resolve respond-by-post court")))
            .isEqualTo(FailureReason.NO_COURT_LOCATION);
    }

    @Test
    void mapsFetchFailedFromTruncatedMessage() {
        assertThat(FailureReasons.from(new IllegalStateException("download was truncated")))
            .isEqualTo(FailureReason.DOCUMENT_FETCH_FAILED);
    }

    @Test
    void mapsUnknownForUnrecognisedException() {
        assertThat(FailureReasons.from(new RuntimeException("something odd")))
            .isEqualTo(FailureReason.UNKNOWN);
    }

    @Test
    void mapsNullPointerWithNullMessageToUnknownWithoutCrashing() {
        // A code bug (NPE) is still classified (as UNKNOWN) and must not crash the classifier on a null message.
        assertThat(FailureReasons.from(new NullPointerException())).isEqualTo(FailureReason.UNKNOWN);
    }

    @Test
    void mapsNullPointerWithMessageToUnknown() {
        assertThat(FailureReasons.from(new NullPointerException("party.getId() is null")))
            .isEqualTo(FailureReason.UNKNOWN);
    }

    @Test
    void mapsNullCauseToUnknownWithoutCrashing() {
        assertThat(FailureReasons.from(null)).isEqualTo(FailureReason.UNKNOWN);
    }
}
