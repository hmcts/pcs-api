package uk.gov.hmcts.reform.pcs.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
import java.util.List;

@Slf4j
@Component
public class IdamAuthenticationFilter extends OncePerRequestFilter {

    private static final List<String> FILTER_PATHS = List.of("/courts", "/ccd");

    private final IdamService idamService;

    public IdamAuthenticationFilter(IdamService idamService) {
        this.idamService = idamService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return FILTER_PATHS.stream()
            .noneMatch(path -> request.getRequestURI().startsWith(path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authToken = request.getHeader(HttpHeaders.AUTHORIZATION);

        try {
            User user = idamService.validateAuthToken(authToken);
            Authentication authentication =
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (InvalidAuthTokenException ex) {
            log.error("Authorization failed: {}", ex.getMessage(), ex);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
        }
    }

}
