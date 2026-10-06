package uk.gov.hmcts.reform.pcs.ccd.service.bundling;

import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.bundling.api.BundleExecutionContext;
import uk.gov.hmcts.ccd.sdk.bundling.api.DocumentReference;
import uk.gov.hmcts.ccd.sdk.bundling.api.DocumentResolver;
import uk.gov.hmcts.ccd.sdk.bundling.api.ResolutionFailure;
import uk.gov.hmcts.ccd.sdk.bundling.api.ResolutionFailureReason;
import uk.gov.hmcts.ccd.sdk.bundling.api.ResolvedDocument;
import uk.gov.hmcts.ccd.sdk.bundling.api.ResolvedDocuments;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

/**
 * Fetches bundle sources from CDAM as the system user, the same way bulk print does. A pcs
 * document's documentId is its CDAM id, so references need no translation.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "ccd.bundling.job", name = "enabled")
public class CdamBundleDocumentResolver implements DocumentResolver {

    private static final String ORIGINAL_FILE_NAME_HEADER = "OriginalFileName";

    private final CaseDocumentClientApi caseDocumentClientApi;
    private final AuthTokenGenerator authTokenGenerator;
    private final IdamTokenProvider systemUpdateUserTokenProvider;

    public CdamBundleDocumentResolver(CaseDocumentClientApi caseDocumentClientApi,
                                      AuthTokenGenerator authTokenGenerator,
                                      @Qualifier("systemUpdateUserTokenProvider")
                                      IdamTokenProvider systemUpdateUserTokenProvider) {
        this.caseDocumentClientApi = caseDocumentClientApi;
        this.authTokenGenerator = authTokenGenerator;
        this.systemUpdateUserTokenProvider = systemUpdateUserTokenProvider;
    }

    @Override
    public String provider() {
        return CaseBundleScope.CDAM_PROVIDER;
    }

    @Override
    public ResolvedDocuments resolveAll(List<DocumentReference> references, BundleExecutionContext context) {
        String authorisation = systemUpdateUserTokenProvider.getAuthToken();
        String serviceAuthorisation = authTokenGenerator.generate();
        Map<DocumentReference, ResolvedDocument> resolved = new LinkedHashMap<>();
        Map<DocumentReference, ResolutionFailure> failures = new LinkedHashMap<>();
        for (DocumentReference reference : references) {
            try {
                ResponseEntity<Resource> response = caseDocumentClientApi.getDocumentBinary(
                    authorisation, serviceAuthorisation, UUID.fromString(reference.id()));
                if (response.getBody() == null) {
                    failures.put(reference, new ResolutionFailure(
                        ResolutionFailureReason.INVALID_CONTENT, "CDAM returned no content"));
                } else {
                    resolved.put(reference, new CdamDocument(reference, response));
                }
            } catch (IllegalArgumentException e) {
                failures.put(reference, new ResolutionFailure(
                    ResolutionFailureReason.NOT_FOUND, "Not a CDAM document id"));
            } catch (FeignException e) {
                failures.put(reference, failure(e));
            }
        }
        return new ResolvedDocuments(resolved, failures);
    }

    private static ResolutionFailure failure(FeignException e) {
        if (e instanceof RetryableException || e.status() >= 500 || e.status() == 429 || e.status() < 0) {
            return new ResolutionFailure(ResolutionFailureReason.TRANSIENT_FAILURE, "CDAM unavailable");
        }
        if (e.status() == 404) {
            return new ResolutionFailure(ResolutionFailureReason.NOT_FOUND, "Not found in CDAM");
        }
        if (e.status() == 401 || e.status() == 403) {
            return new ResolutionFailure(ResolutionFailureReason.ACCESS_DENIED, "CDAM refused access");
        }
        return new ResolutionFailure(ResolutionFailureReason.INVALID_CONTENT, "CDAM rejected the request");
    }

    private static final class CdamDocument implements ResolvedDocument {

        private final DocumentReference reference;
        private final Resource body;
        private final HttpHeaders headers;
        private InputStream stream;

        CdamDocument(DocumentReference reference, ResponseEntity<Resource> response) {
            this.reference = reference;
            this.body = response.getBody();
            this.headers = response.getHeaders();
        }

        @Override
        public InputStream content() {
            try {
                stream = body.getInputStream();
                return stream;
            } catch (IOException e) {
                throw new UncheckedIOException("Could not read document " + reference.id() + " from CDAM", e);
            }
        }

        @Override
        public String mediaType() {
            // The SDK detects the real type from the content; this is only the declared one, so an
            // unparseable header must not fail the bundle.
            MediaType contentType;
            try {
                contentType = headers.getContentType();
            } catch (InvalidMediaTypeException e) {
                contentType = null;
            }
            return contentType == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : contentType.getType() + "/" + contentType.getSubtype();
        }

        @Override
        public String fileName() {
            return Optional.ofNullable(headers.getFirst(ORIGINAL_FILE_NAME_HEADER)).orElse(reference.id());
        }

        @Override
        public OptionalLong contentLength() {
            long length = headers.getContentLength();
            return length >= 0 ? OptionalLong.of(length) : OptionalLong.empty();
        }

        @Override
        public Optional<String> checksum() {
            return Optional.empty();
        }

        @Override
        public void close() throws IOException {
            if (stream != null) {
                stream.close();
            }
        }
    }
}
