package uk.gov.hmcts.reform.pcs.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Clears the per-request MDC context at the end of each request so the
 * caseId added by handlers does not leak across pooled threads. The
 * Application Insights Java agent promotes MDC entries to customDimensions.
 */
public class RequestInterceptor implements HandlerInterceptor {

    public static final String CASE_ID = "caseId";

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) {
        MDC.remove(CASE_ID);
    }
}
