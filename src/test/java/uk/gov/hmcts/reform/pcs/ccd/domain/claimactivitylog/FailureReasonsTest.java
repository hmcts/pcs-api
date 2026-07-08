package uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog;

import feign.FeignException;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.BulkPrintMergeException;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.MissingPostalAddressException;
import uk.gov.hmcts.reform.pcs.document.service.exception.DocAssemblyException;
import uk.gov.hmcts.reform.pcs.document.service.exception.DocumentStoreException;
import uk.gov.hmcts.reform.pcs.exception.DocumentDownloadException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class FailureReasonsTest {

    @Test
    void mapsMissingAddress() {
        assertThat(FailureReasons.from(new MissingPostalAddressException("no address")))
            .isEqualTo(FailureReason.MISSING_ADDRESS);
    }

    @Test
    void mapsRenderFailed() {
        assertThat(FailureReasons.from(new DocAssemblyException("docmosis 500")))
            .isEqualTo(FailureReason.RENDER_FAILED);
    }

    @Test
    void mapsMergeFailed() {
        assertThat(FailureReasons.from(new BulkPrintMergeException("merge", new RuntimeException())))
            .isEqualTo(FailureReason.MERGE_FAILED);
    }

    @Test
    void mapsDocumentFetchFailed() {
        assertThat(FailureReasons.from(new DocumentDownloadException("fetch", new RuntimeException())))
            .isEqualTo(FailureReason.DOCUMENT_FETCH_FAILED);
    }

    @Test
    void mapsDocumentStoreFailed() {
        assertThat(FailureReasons.from(new DocumentStoreException("cdam down", new RuntimeException())))
            .isEqualTo(FailureReason.DOCUMENT_STORE_FAILED);
    }

    @Test
    void storeFailureWrappingFeignIsClassifiedAsStoreNotSend() {
        // The store failure wraps a FeignException from CDAM; it must NOT fall through to the
        // FeignException -> SEND_LETTER_UNAVAILABLE catch-all.
        DocumentStoreException storeFailure = new DocumentStoreException("cdam", mock(FeignException.class));
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
}
