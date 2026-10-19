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
 * Deletes the temp directory a batch print was built in once the request that built it has finished, however
 * it finished.
 *
 * <p>A batch print is streamed from a temp file rather than held in memory, so something has to delete it.
 * Deleting it as the response body is closed is not enough: Spring never writes the body if the bailiff's
 * client has already gone (a reset connection while the headers flush) or if anything further out of the
 * chain throws, and each abandoned file can hold 50 cases' documents with nothing to sweep it up. Deleting on
 * request completion covers the body being written, not being written, and the request failing.</p>
 */
@Component
public class BatchPrintFileCleanupFilter extends OncePerRequestFilter {

    private static final String REQUEST_ATTRIBUTE = BatchPrintFileCleanupFilter.class.getName();

    /**
     * Asks for this directory and the batch print in it to be deleted when the current request finishes.
     * Called before the file is handed to Spring to stream.
     */
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
