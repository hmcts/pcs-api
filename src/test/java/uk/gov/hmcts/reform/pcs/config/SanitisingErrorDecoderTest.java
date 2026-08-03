package uk.gov.hmcts.reform.pcs.config;

import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.exception.ErrorCode;
import uk.gov.hmcts.reform.pcs.exception.RemoteCallException;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class SanitisingErrorDecoderTest {

    private SanitisingErrorDecoder underTest;

    @BeforeEach
    void setUp() {
        underTest = new SanitisingErrorDecoder();
    }

    @Test
    void shouldDecodeToRemoteCallExceptionWithBody() {
        // Given
        Response response = buildResponse(500, "error");

        // When
        Exception exception = underTest.decode("Client#call()", response);

        // Then
        assertThat(exception).isInstanceOf(RemoteCallException.class);
        RemoteCallException remoteCallException = (RemoteCallException) exception;
        assertThat(remoteCallException.getStatus()).isEqualTo(500);
        assertThat(remoteCallException.getContext().getValue("Remote call")).contains("Client#call()");
        assertThat(remoteCallException.getContext().getValue("Status")).contains(500);
        assertThat(remoteCallException.getContext().getValue("Response Body")).contains("error");
    }

    @Test
    void shouldReturnEmptyMarkerWhenBodyNull() {
        // Given
        Response response = Response.builder().status(404).request(request()).headers(Collections.emptyMap()).build();

        // When
        RemoteCallException exception = (RemoteCallException) underTest.decode("Client#get()", response);

        // Then
        assertThat(exception.getContext().getValue("Response Body")).contains("<empty>");
    }

    @Test
    void shouldTruncateLargeBody() {
        // Given
        String largeBody = "x".repeat(5000);
        Response response = buildResponse(500, largeBody);

        // When
        RemoteCallException exception = (RemoteCallException) underTest.decode("Client#get()", response);

        // Then
        String body = (String) exception.getContext().getValue("Response Body").orElseThrow();
        assertThat(body).endsWith("…(truncated)");
        assertThat(body).hasSize(4096 + "…(truncated)".length());
    }

    @Test
    void shouldPreserveErrorCode() {
        // Given
        Response response = buildResponse(400, "bad");

        // When
        RemoteCallException exception = (RemoteCallException) underTest.decode("Client#post()", response);

        // Then
        assertThat(exception.getCode()).isEqualTo(ErrorCode.REMOTE_CALL);
    }

    private Response buildResponse(int status, String body) {
        return Response.builder().status(status).request(request()).headers(Collections.emptyMap())
            .body(body, StandardCharsets.UTF_8).build();
    }

    private Request request() {
        return Request.create(Request.HttpMethod.GET, "http://localhost", Collections.emptyMap(),
                              Request.Body.empty(), null);
    }
}
