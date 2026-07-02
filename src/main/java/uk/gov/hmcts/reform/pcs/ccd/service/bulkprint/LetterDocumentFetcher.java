package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;
import uk.gov.hmcts.reform.sendletter.api.model.v3.Document;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.UUID;

/**
 * Fetches a stored document from CDAM and returns it as a base64 Send Letter {@link Document}. Authenticates
 * as the system user (CDAM needs a user token) plus S2S, so it is safe to call from the scheduled sweep.
 */
@Service
public class LetterDocumentFetcher {

    private static final int SINGLE_COPY = 1;

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

    public Document fetch(UUID documentId) {
        Resource resource = caseDocumentClientApi.getDocumentBinary(
            systemUpdateUserTokenProvider.getAuthToken(),
            authTokenGenerator.generate(),
            documentId
        ).getBody();
        return new Document(Base64.getEncoder().encodeToString(readAllBytes(resource)), SINGLE_COPY);
    }

    private byte[] readAllBytes(Resource resource) {
        try {
            return resource.getInputStream().readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read document content for bulk print", e);
        }
    }
}
