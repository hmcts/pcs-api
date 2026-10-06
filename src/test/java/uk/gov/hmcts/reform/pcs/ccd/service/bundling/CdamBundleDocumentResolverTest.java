package uk.gov.hmcts.reform.pcs.ccd.service.bundling;

import feign.FeignException;
import feign.Request;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.ccd.sdk.bundling.api.BundleExecutionContext;
import uk.gov.hmcts.ccd.sdk.bundling.api.DocumentReference;
import uk.gov.hmcts.ccd.sdk.bundling.api.ResolutionFailureReason;
import uk.gov.hmcts.ccd.sdk.bundling.api.ResolvedDocument;
import uk.gov.hmcts.ccd.sdk.bundling.api.ResolvedDocuments;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CdamBundleDocumentResolverTest {

    private static final String AUTH = "Bearer system-user";
    private static final String S2S = "s2s-token";
    private static final UUID CDAM_ID = UUID.randomUUID();
    private static final DocumentReference REFERENCE =
        new DocumentReference(CaseBundleScope.CDAM_PROVIDER, CDAM_ID.toString());

    @Mock
    private CaseDocumentClientApi caseDocumentClientApi;
    @Mock
    private AuthTokenGenerator authTokenGenerator;
    @Mock
    private IdamTokenProvider systemUpdateUserTokenProvider;

    private CdamBundleDocumentResolver underTest;

    @BeforeEach
    void setUp() {
        underTest = new CdamBundleDocumentResolver(caseDocumentClientApi, authTokenGenerator,
                                                   systemUpdateUserTokenProvider);
        lenient().when(systemUpdateUserTokenProvider.getAuthToken()).thenReturn(AUTH);
        lenient().when(authTokenGenerator.generate()).thenReturn(S2S);
    }

    @Test
    void shouldResolveCdamDocumentsByProvider() {
        assertThat(underTest.provider()).isEqualTo(CaseBundleScope.CDAM_PROVIDER);
    }

    @Test
    void shouldFetchTheDocumentFromCdamAsTheSystemUser() throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "application/pdf; charset=UTF-8");
        headers.add("OriginalFileName", "claim.pdf");
        headers.setContentLength(7);
        when(caseDocumentClientApi.getDocumentBinary(AUTH, S2S, CDAM_ID))
            .thenReturn(new ResponseEntity<>(new ByteArrayResource(bytes("content")), headers,
                                             HttpStatus.OK));

        try (ResolvedDocument document = resolveOne()) {
            assertThat(document.mediaType()).isEqualTo("application/pdf");
            assertThat(document.fileName()).isEqualTo("claim.pdf");
            assertThat(document.contentLength()).hasValue(7);
            assertThat(document.checksum()).isEmpty();
            try (InputStream content = document.content()) {
                assertThat(content.readAllBytes()).isEqualTo(bytes("content"));
            }
        }
    }

    @Test
    void shouldFallBackWhenCdamDeclaresNoTypeNameOrLength() throws IOException {
        when(caseDocumentClientApi.getDocumentBinary(AUTH, S2S, CDAM_ID))
            .thenReturn(ResponseEntity.ok(new ByteArrayResource(bytes("content"))));

        try (ResolvedDocument document = resolveOne()) {
            assertThat(document.mediaType()).isEqualTo("application/octet-stream");
            assertThat(document.fileName()).isEqualTo(CDAM_ID.toString());
            assertThat(document.contentLength()).isEmpty();
        }
    }

    @Test
    void shouldNotFailTheDocumentOverAnUnparseableContentType() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "application/pdf; charset=binary");
        when(caseDocumentClientApi.getDocumentBinary(AUTH, S2S, CDAM_ID))
            .thenReturn(new ResponseEntity<>(new ByteArrayResource(bytes("content")), headers, HttpStatus.OK));

        assertThat(resolveOne().mediaType()).isEqualTo("application/octet-stream");
    }

    @Test
    void shouldCloseTheStreamItOpened() throws IOException {
        Resource body = mock(Resource.class);
        InputStream stream = mock(InputStream.class);
        when(body.getInputStream()).thenReturn(stream);
        when(caseDocumentClientApi.getDocumentBinary(AUTH, S2S, CDAM_ID)).thenReturn(ResponseEntity.ok(body));

        ResolvedDocument document = resolveOne();
        document.content();
        document.close();

        verify(stream).close();
    }

    @Test
    void shouldReportAnUnreadableBody() throws IOException {
        Resource body = mock(Resource.class);
        when(body.getInputStream()).thenThrow(new IOException("connection reset"));
        when(caseDocumentClientApi.getDocumentBinary(AUTH, S2S, CDAM_ID)).thenReturn(ResponseEntity.ok(body));

        ResolvedDocument document = resolveOne();

        assertThatThrownBy(document::content)
            .isInstanceOf(UncheckedIOException.class)
            .hasMessageContaining(CDAM_ID.toString());
    }

    @Test
    void shouldFailADocumentWithNoContent() {
        when(caseDocumentClientApi.getDocumentBinary(AUTH, S2S, CDAM_ID)).thenReturn(ResponseEntity.ok().build());

        assertThat(failureReason(resolve(REFERENCE))).isEqualTo(ResolutionFailureReason.INVALID_CONTENT);
    }

    @Test
    void shouldFailAReferenceThatIsNotACdamId() {
        DocumentReference notAnId = new DocumentReference(CaseBundleScope.CDAM_PROVIDER, "not-a-uuid");

        ResolvedDocuments resolved = underTest.resolveAll(List.of(notAnId), BundleExecutionContext.empty());

        assertThat(resolved.failures().get(notAnId).reason()).isEqualTo(ResolutionFailureReason.NOT_FOUND);
    }

    @ParameterizedTest
    @CsvSource({
        "500, TRANSIENT_FAILURE",
        "503, TRANSIENT_FAILURE",
        "429, TRANSIENT_FAILURE",
        "404, NOT_FOUND",
        "401, ACCESS_DENIED",
        "403, ACCESS_DENIED",
        "400, INVALID_CONTENT"
    })
    void shouldClassifyCdamErrors(int status, ResolutionFailureReason expected) {
        when(caseDocumentClientApi.getDocumentBinary(AUTH, S2S, CDAM_ID))
            .thenThrow(FeignException.errorStatus("getDocumentBinary", feignResponse(status)));

        assertThat(failureReason(resolve(REFERENCE))).isEqualTo(expected);
    }

    @Test
    void shouldTreatAnUnreachableCdamAsTransient() {
        when(caseDocumentClientApi.getDocumentBinary(AUTH, S2S, CDAM_ID))
            .thenThrow(new RetryableException(-1, "timed out", Request.HttpMethod.GET, (Long) null, request()));

        assertThat(failureReason(resolve(REFERENCE))).isEqualTo(ResolutionFailureReason.TRANSIENT_FAILURE);
    }

    @Test
    void shouldResolveTheRestWhenOneDocumentFails() {
        UUID missingId = UUID.randomUUID();
        DocumentReference missing = new DocumentReference(CaseBundleScope.CDAM_PROVIDER, missingId.toString());
        when(caseDocumentClientApi.getDocumentBinary(AUTH, S2S, missingId))
            .thenThrow(FeignException.errorStatus("getDocumentBinary", feignResponse(404)));
        when(caseDocumentClientApi.getDocumentBinary(AUTH, S2S, CDAM_ID))
            .thenReturn(ResponseEntity.ok(new ByteArrayResource(bytes("content"))));

        ResolvedDocuments resolved = resolve(missing, REFERENCE);

        assertThat(resolved.resolved()).containsOnlyKeys(REFERENCE);
        assertThat(resolved.failures()).containsOnlyKeys(missing);
    }

    private ResolvedDocument resolveOne() {
        ResolvedDocuments resolved = resolve(REFERENCE);
        assertThat(resolved.failures()).isEmpty();
        return resolved.resolved().get(REFERENCE);
    }

    private ResolvedDocuments resolve(DocumentReference... references) {
        return underTest.resolveAll(List.of(references), BundleExecutionContext.empty());
    }

    private static ResolutionFailureReason failureReason(ResolvedDocuments resolved) {
        assertThat(resolved.resolved()).isEmpty();
        return resolved.failures().get(REFERENCE).reason();
    }

    private static feign.Response feignResponse(int status) {
        return feign.Response.builder()
            .status(status)
            .reason("status " + status)
            .request(request())
            .headers(Map.of())
            .build();
    }

    private static Request request() {
        return Request.create(Request.HttpMethod.GET, "http://cdam/cases/documents/" + CDAM_ID + "/binary",
                              Map.of(), null, StandardCharsets.UTF_8, null);
    }

    private static byte[] bytes(String content) {
        return content.getBytes(StandardCharsets.UTF_8);
    }
}
