package uk.gov.hmcts.reform.pcs.bulkclaim.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.hmcts.reform.authorisation.exceptions.InvalidTokenException;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.pcs.bulkclaim.config.BulkClaimProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BulkClaimApimS2sFilterTest {

    private static final String APIM = "pcs_bulk_claims_apim";
    private static final String TOKEN = "Bearer apim-token";
    private static final String OTHER_TOKEN = "Bearer other-token";

    @Mock
    private AuthTokenValidator validator;
    @Mock
    private FilterChain chain;

    private BulkClaimApimS2sFilter filter;

    @BeforeEach
    void setUp() {
        filter = new BulkClaimApimS2sFilter(validator, new BulkClaimProperties());
    }

    @Test
    void shouldAllowApimOnBulkPathWithOrganisationHeader() throws Exception {
        when(validator.getServiceName(TOKEN)).thenReturn(APIM);
        MockHttpServletRequest request = request("POST", "/v2/bulk-claims/services/BulkClaim", TOKEN);
        request.addHeader("X-Organisation-Id", " ORG123 ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(request.getAttribute(BulkClaimApimS2sFilter.ORGANISATION_ID_ATTRIBUTE)).isEqualTo("ORG123");
    }

    @Test
    void shouldRejectApimOnBulkPathWithoutOrganisationHeader() throws Exception {
        when(validator.getServiceName(TOKEN)).thenReturn(APIM);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("POST", "/v2/bulk-claims/services/BulkClaim", TOKEN), response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).isEqualTo("Missing organisation");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void shouldRejectApimOnBulkPathWithBlankOrganisationHeader() throws Exception {
        when(validator.getServiceName(TOKEN)).thenReturn(APIM);
        MockHttpServletRequest request = request("POST", "/v2/bulk-claims/services/BulkClaim", TOKEN);
        request.addHeader("X-Organisation-Id", "  ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void shouldRejectApimOnCcdPersistencePath() throws Exception {
        when(validator.getServiceName(TOKEN)).thenReturn(APIM);
        MockHttpServletRequest request = request("POST", "/ccd-persistence/cases", TOKEN);
        request.addHeader("X-Organisation-Id", "ORG123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void shouldRejectApimOnPaymentUpdatePath() throws Exception {
        when(validator.getServiceName(TOKEN)).thenReturn(APIM);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("PUT", "/payment-update", TOKEN), response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void shouldRejectOtherWhitelistedServiceOnBulkPath() throws Exception {
        when(validator.getServiceName(OTHER_TOKEN)).thenReturn("xui_webapp");
        MockHttpServletRequest request = request("POST", "/v2/bulk-claims/services/BulkClaim", OTHER_TOKEN);
        request.addHeader("X-Organisation-Id", "ORG123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void shouldIgnoreOrganisationHeaderFromNonApimCaller() throws Exception {
        when(validator.getServiceName(OTHER_TOKEN)).thenReturn("pcs_frontend");
        MockHttpServletRequest request = request("GET", "/cases/1", OTHER_TOKEN);
        request.addHeader("X-Organisation-Id", "ORG123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(request.getAttribute(BulkClaimApimS2sFilter.ORGANISATION_ID_ATTRIBUTE)).isNull();
    }

    @Test
    void shouldLeaveRequestWithoutServiceTokenOnNonBulkPathUntouched() throws Exception {
        MockHttpServletRequest request = request("GET", "/health", null);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(validator, never()).getServiceName(any());
    }

    @Test
    void shouldRejectBulkPathWithoutServiceToken() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("POST", "/v2/bulk-claims/services/BulkClaim", null), response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void shouldRejectUnresolvableToken() throws Exception {
        when(validator.getServiceName(TOKEN)).thenThrow(new InvalidTokenException("bad"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("POST", "/v2/bulk-claims/services/BulkClaim", TOKEN), response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void shouldCacheServiceNameAcrossRequests() throws Exception {
        when(validator.getServiceName(TOKEN)).thenReturn(APIM);

        for (int i = 0; i < 2; i++) {
            MockHttpServletRequest request = request("POST", "/v2/bulk-claims/services/BulkClaim", TOKEN);
            request.addHeader("X-Organisation-Id", "ORG123");
            filter.doFilter(request, new MockHttpServletResponse(), chain);
        }

        verify(validator, times(1)).getServiceName(TOKEN);
    }

    @Test
    void shouldAddBearerPrefixWhenMissing() throws Exception {
        when(validator.getServiceName(TOKEN)).thenReturn("pcs_frontend");

        filter.doFilter(request("GET", "/cases/1", "apim-token"), new MockHttpServletResponse(), chain);

        verify(validator).getServiceName(TOKEN);
    }

    private static MockHttpServletRequest request(String method, String uri, String token) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        if (token != null) {
            request.addHeader("ServiceAuthorization", token);
        }
        return request;
    }
}
