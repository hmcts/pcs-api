package uk.gov.hmcts.reform.pcs.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.gov.hmcts.reform.pcs.exception.InvalidAuthTokenException;
import uk.gov.hmcts.reform.pcs.idam.IdamService;
import uk.gov.hmcts.reform.pcs.idam.User;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
public class IdamAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private  IdamService idamService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (!path.startsWith("/courts")) {
            filterChain.doFilter(request, response);
            return;
        }
        String authToken = request.getHeader("Authorization");

        try {
            User user = idamService.validateAuthToken(authToken);
            Authentication authentication =
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (InvalidAuthTokenException ex) {
            log.error("Authentication failed: {}", ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            String json = "{\"error\":\"" + ex.getMessage() + "\"}";
            response.getWriter().write(json);
        }
    }

}
