package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

/**
 * Fetches a stored document's bytes from CDAM. Authenticates as the system user (CDAM needs a user token)
 * plus S2S, so it is safe to call from the scheduled sweep. The bytes are merged into the letter PDF by
 * {@link PdfMerger} rather than sent as a separate Send Letter document.
 */
@Service
public class LetterDocumentFetcher {

    private final CaseDocumentClientApi caseDocumentClientApi;
    private final AuthTokenGenerator authTokenGenerator;
    private final IdamTokenProvider systemUpdateUserTokenProvider;

    public LetterDocumentFetcher(CaseDocumentClientApi caseDocumentClientApi,
                                 AuthTokenGenerator authTokenGenerator,
                                 @Qualifier("systemUpdateUserTokenProvider")
                                 IdamTokenProvider systemUpdateUserTokenProvider) {
        this.caseDocumentClientApi = caseDocumentClientApi;
        this.authTokenGenerator = authTokenGenerator;
        this.systemUpdateUserTokenProvider = systemUpdateUserTokenProvider;
    }

    public byte[] fetchBytes(UUID documentId) {
        ResponseEntity<Resource> response = caseDocumentClientApi.getDocumentBinary(
            systemUpdateUserTokenProvider.getAuthToken(),
            authTokenGenerator.generate(),
            documentId
        );
        byte[] content = readAllBytes(response.getBody());
        // Reject a truncated download rather than posting a partial PDF (Content-Length is -1 when unknown).
        long expectedLength = response.getHeaders().getContentLength();
        if (expectedLength >= 0 && expectedLength != content.length) {
            throw new IllegalStateException("Document " + documentId + " truncated: expected " + expectedLength
                + " bytes but read " + content.length);
        }
        return content;
    }

    private byte[] readAllBytes(Resource resource) {
        try {
            return resource.getInputStream().readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read document content for bulk print", e);
        }
    }
}
