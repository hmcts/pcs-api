package uk.gov.hmcts.reform.pcs.config;

import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.exception.RemoteCallException;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SanitisingErrorDecoderTest {

    private static final String EXCEPTION_BODY = "PII details like email address etc etc";
    private SanitisingErrorDecoder underTest;

    @BeforeEach
    void setUp() {
        underTest = new SanitisingErrorDecoder();
    }

    @Test
    void shouldDecodeToRemoteCallExceptionWithBody() {
        // Given
        String methodKey = "xyz";
        Response response = buildResponse();

        // When
        Exception exception = underTest.decode(methodKey, response);

        // Then
        assertThat(exception).isInstanceOf(RemoteCallException.class);
        RemoteCallException remoteCallException = (RemoteCallException) exception;
        assertThat(remoteCallException.getStatus()).isEqualTo(500);
        assertThat(remoteCallException.getContext().getValue("MethodKey")).contains(methodKey);
        assertThat(remoteCallException.getContext().getValue("Status")).contains(500);
        assertThat(remoteCallException.getContext().getValue("MethodKey")).contains(methodKey);
        assertThat(remoteCallException.getContext().getValue("Status")).contains(500);
        assertThat(remoteCallException.getContext().getValue("Target")).contains("http://localhost");
        assertThat(remoteCallException.getContext().getValue("Method")).contains(Request.HttpMethod.GET);
        assertThat(remoteCallException.getContext().getValue("Reason")).contains("Internal Server Error");
        assertThat(remoteCallException.getContext().getValue("Content-Type")).contains("application/json");
        assertThat(remoteCallException.getContext().getValue("Content-Length")).contains("5");
        assertThat(remoteCallException.getContext().getValue("Correlation Id")).contains("123");
        assertThat(remoteCallException.getContext().getValue("Retry-After")).contains("120");
        assertThat(remoteCallException.getContext().asDebugString()).doesNotContain(EXCEPTION_BODY);
    }

    private Response buildResponse() {
        Map<String, Collection<String>> headers = Map.of(
            "content-type", List.of("application/json"),
            "content-length", List.of("5"),
            "x-correlation-id", List.of("123"),
            "retry-after", List.of("120")
        );
        return Response.builder().status(500).reason("Internal Server Error").request(request())
            .headers(headers).body(EXCEPTION_BODY, StandardCharsets.UTF_8).build();
    }

    private Request request() {
        return Request.create(Request.HttpMethod.GET, "http://localhost", Collections.emptyMap(),
                              Request.Body.empty(), null);
    }
}
