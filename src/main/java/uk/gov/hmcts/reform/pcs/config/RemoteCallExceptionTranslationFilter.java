package uk.gov.hmcts.reform.pcs.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.gov.hmcts.reform.pcs.exception.RemoteCallException;

import java.io.IOException;
import java.util.Map;

@Component
public class RemoteCallExceptionTranslationFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    public RemoteCallExceptionTranslationFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (RemoteCallException ex) {
            int downstream = ex.getStatus();   // status preserved by SanitisingErrorDecoder
            int translated = downstream == HttpStatus.UNAUTHORIZED.value()
                ? HttpStatus.UNAUTHORIZED.value()
                : HttpStatus.INTERNAL_SERVER_ERROR.value();

            response.setStatus(translated);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), Map.of(
                "status", translated,
                "error", HttpStatus.valueOf(translated).getReasonPhrase()
            ));
        }
    }

}
