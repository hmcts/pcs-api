package uk.gov.hmcts.reform.pcs.enforcement.batchprint;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Deletes a batch print's temp directory on request completion, not as the response body is closed: Spring
 * never writes the body if the client has gone or the chain throws.
 */
@Component
public class BatchPrintFileCleanupFilter extends OncePerRequestFilter {

    private static final String REQUEST_ATTRIBUTE = BatchPrintFileCleanupFilter.class.getName();

    static void deleteWhenRequestFinishes(HttpServletRequest request, Path directory) {
        request.setAttribute(REQUEST_ATTRIBUTE, directory);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } finally {
            if (request.getAttribute(REQUEST_ATTRIBUTE) instanceof Path directory) {
                BatchPrintTempDirectory.delete(directory);
            }
        }
    }
}
