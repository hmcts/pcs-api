package uk.gov.hmcts.reform.pcs.bulkclaim.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.gov.hmcts.reform.authorisation.exceptions.InvalidTokenException;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.pcs.bulkclaim.config.BulkClaimProperties;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

/**
 * Locks the APIM S2S identity to {@code /v2/bulk-claims/**} and every other caller out of it. Must run after
 * {@code ServiceAuthFilter}, which validates the S2S token. The organisation id is only trusted from the APIM
 * header and is exposed to controllers solely through {@link #ORGANISATION_ID_ATTRIBUTE}.
 */
@Slf4j
public class BulkClaimApimS2sFilter extends OncePerRequestFilter {

    public static final String ORGANISATION_ID_ATTRIBUTE = BulkClaimApimS2sFilter.class.getName()
        + ".organisationId";
    public static final String BULK_CLAIMS_PATH = "/v2/bulk-claims";

    private static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final Duration SERVICE_NAME_CACHE_TTL = Duration.ofMinutes(5);
    private static final long SERVICE_NAME_CACHE_MAX_SIZE = 1000;

    private final AuthTokenValidator authTokenValidator;
    private final BulkClaimProperties properties;
    private final Cache<String, String> serviceNameCache = Caffeine.newBuilder()
        .expireAfterWrite(SERVICE_NAME_CACHE_TTL)
        .maximumSize(SERVICE_NAME_CACHE_MAX_SIZE)
        .build();

    public BulkClaimApimS2sFilter(AuthTokenValidator authTokenValidator, BulkClaimProperties properties) {
        this.authTokenValidator = authTokenValidator;
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        boolean bulkPath = isBulkClaimsPath(request);
        String token = request.getHeader(SERVICE_AUTHORIZATION);

        if (token == null || token.isBlank()) {
            if (bulkPath) {
                reject(response, HttpStatus.FORBIDDEN, "Forbidden");
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        String serviceName;
        try {
            serviceName = resolveServiceName(token);
        } catch (InvalidTokenException e) {
            log.warn("Rejecting request with unresolvable S2S token");
            reject(response, HttpStatus.FORBIDDEN, "Forbidden");
            return;
        } catch (RuntimeException e) {
            log.error("Unable to resolve S2S service name", e);
            reject(response, HttpStatus.SERVICE_UNAVAILABLE, "Service unavailable");
            return;
        }

        boolean apimCaller = properties.getApimServiceNames().contains(serviceName);

        if (bulkPath != apimCaller) {
            log.warn("Rejecting S2S caller '{}' for path '{}'", serviceName, request.getRequestURI());
            reject(response, HttpStatus.FORBIDDEN, "Forbidden");
            return;
        }

        if (bulkPath) {
            String organisationId = request.getHeader(properties.getOrganisationHeader());
            if (organisationId == null || organisationId.isBlank()) {
                reject(response, HttpStatus.FORBIDDEN, "Missing organisation");
                return;
            }
            request.setAttribute(ORGANISATION_ID_ATTRIBUTE, organisationId.trim());
        }

        filterChain.doFilter(request, response);
    }

    private boolean isBulkClaimsPath(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.equals(BULK_CLAIMS_PATH) || path.startsWith(BULK_CLAIMS_PATH + "/");
    }

    private String resolveServiceName(String token) {
        String bearerToken = token.startsWith(BEARER_PREFIX) ? token : BEARER_PREFIX + token;
        return serviceNameCache.get(sha256(bearerToken), key -> authTokenValidator.getServiceName(bearerToken));
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static void reject(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.getWriter().write(message);
    }
}
