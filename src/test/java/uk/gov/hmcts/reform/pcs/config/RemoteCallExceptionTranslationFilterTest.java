package uk.gov.hmcts.reform.pcs.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.hmcts.reform.pcs.exception.RedactionContext;
import uk.gov.hmcts.reform.pcs.exception.RemoteCallException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.pcs.exception.ErrorCode.REMOTE_CALL;

public class RemoteCallExceptionTranslationFilterTest {

    private ObjectMapper objectMapper;
    private RemoteCallExceptionTranslationFilter underTest;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        underTest = new RemoteCallExceptionTranslationFilter(objectMapper);
    }

    @Test
    void shouldPassThroughWhenNoRemoteCallExceptionThrown() throws Exception {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        // When
        underTest.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        assertThat(200).isEqualTo(response.getStatus());
        assertThat(response.getContentType()).isNull();
        assertThat("").isEqualTo(response.getContentAsString());
    }

    @Test
    void shouldReturn401WhenRemoteCallExceptionStatusIsUnauthorized() throws Exception {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        RemoteCallException ex = new RemoteCallException(REMOTE_CALL,
                                                         RedactionContext.of("error","downstream 401"),
                                                         HttpStatus.UNAUTHORIZED.value());
        doThrow(ex).when(filterChain).doFilter(request, response);

        // When
        underTest.doFilterInternal(request, response, filterChain);

        // Then
        assertThat(HttpStatus.UNAUTHORIZED.value()).isEqualTo(response.getStatus());
        assertThat(MediaType.APPLICATION_JSON_VALUE).isEqualTo(response.getContentType());
        Map<String, Object> body = objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
        assertThat(HttpStatus.UNAUTHORIZED.value()).isEqualTo(body.get("status"));
        assertThat(HttpStatus.UNAUTHORIZED.getReasonPhrase()).isEqualTo(body.get("error"));
    }

    @Test
    void shouldReturn500WhenRemoteCallExceptionStatusIsNotUnauthorized() throws Exception {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        RemoteCallException ex = new RemoteCallException(REMOTE_CALL,
                                                         RedactionContext.of("error", "downstream 404"),
                                                         HttpStatus.NOT_FOUND.value());
        doThrow(ex).when(filterChain).doFilter(request, response);

        // When
        underTest.doFilterInternal(request, response, filterChain);

        // Then
        assertThat(HttpStatus.INTERNAL_SERVER_ERROR.value()).isEqualTo(response.getStatus());
        assertThat(MediaType.APPLICATION_JSON_VALUE).isEqualTo(response.getContentType());
        Map<String, Object> body = objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
        assertThat(HttpStatus.INTERNAL_SERVER_ERROR.value()).isEqualTo(body.get("status"));
        assertThat(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase()).isEqualTo(body.get("error"));
    }

}
