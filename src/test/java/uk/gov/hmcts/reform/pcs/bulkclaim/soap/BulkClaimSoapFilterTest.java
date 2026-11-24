package uk.gov.hmcts.reform.pcs.bulkclaim.soap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.unit.DataSize;
import uk.gov.hmcts.reform.pcs.bulkclaim.config.BulkClaimProperties;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class BulkClaimSoapFilterTest {

    private static final String ENVELOPE = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">"
        + "<soapenv:Body><a/></soapenv:Body></soapenv:Envelope>";

    @Mock
    private FeatureToggleService featureToggleService;

    private final BulkClaimProperties properties = new BulkClaimProperties();
    private BulkClaimSoapFilter underTest;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        properties.setMaxRequestSize(DataSize.ofBytes(512));
        lenient().when(featureToggleService.isEnabled(FeatureFlag.BULK_CLAIMS_V2)).thenReturn(true);
        underTest = new BulkClaimSoapFilter(featureToggleService, properties);
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    @Test
    void shouldHideEverythingWhenTheFlagIsOff() throws Exception {
        lenient().when(featureToggleService.isEnabled(FeatureFlag.BULK_CLAIMS_V2)).thenReturn(false);

        underTest.doFilter(post(ENVELOPE), response, chain);
        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(chain.getRequest()).isNull();

        response = new MockHttpServletResponse();
        underTest.doFilter(get(BulkClaimSoapFilter.WSDL_PATH), response, chain);
        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void shouldPassAWellFormedEnvelopeOnWithItsBodyIntact() throws Exception {
        MockHttpServletRequest request = post(ENVELOPE);
        request.setAttribute("organisation", "ORG1");

        underTest.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        var forwarded = (jakarta.servlet.http.HttpServletRequest) chain.getRequest();
        assertThat(forwarded.getAttribute("organisation")).isEqualTo("ORG1");
        assertThat(new String(forwarded.getInputStream().readAllBytes(), StandardCharsets.UTF_8)).isEqualTo(ENVELOPE);
        assertThat(forwarded.getContentLength()).isEqualTo(ENVELOPE.length());
    }

    @Test
    void shouldRefuseABodyOverTheLimitWithoutReadingIt() throws Exception {
        MockHttpServletRequest request = post(ENVELOPE);
        request.setContent(new byte[513]);

        underTest.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void shouldRefuseAnUndeclaredLengthOverTheLimit() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", BulkClaimSoapFilter.ENDPOINT_PATH) {
            @Override
            public long getContentLengthLong() {
                return -1;
            }
        };
        request.setContent(("<a>" + "x".repeat(600) + "</a>").getBytes(StandardCharsets.UTF_8));

        underTest.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(413);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "<soapenv:Envelope",
        "",
        "<?xml version=\"1.0\"?><!DOCTYPE e [<!ENTITY x SYSTEM \"file:///etc/passwd\">]><e>&x;</e>",
        "<?xml version=\"1.0\"?><!DOCTYPE e [<!ENTITY a \"aaaa\"><!ENTITY b \"&a;&a;\">]><e>&b;</e>"
    })
    void shouldFaultForMalformedXmlOrADoctype(String body) throws Exception {
        underTest.doFilter(post(body), response, chain);

        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getContentType()).startsWith("text/xml");
        assertThat(response.getContentAsString())
            .contains("<faultcode>soapenv:Client</faultcode>", "not a well-formed SOAP message");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void shouldReportSoap12AsAVersionMismatch() throws Exception {
        underTest.doFilter(post("<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\"><s:Body/>"
            + "</s:Envelope>"), response, chain);

        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getContentAsString()).contains("<faultcode>soapenv:VersionMismatch</faultcode>");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void shouldFaultForXmlThatIsNotASoapEnvelope() throws Exception {
        underTest.doFilter(post("<RentalClaimSet/>"), response, chain);

        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getContentAsString()).contains("not a SOAP 1.1 envelope");
    }

    @Test
    void shouldServeTheWsdlAtBothUrlsWithTheRequestsEndpointAddress() throws Exception {
        MockHttpServletRequest byParameter = get(BulkClaimSoapFilter.ENDPOINT_PATH);
        byParameter.setParameter("wsdl", "");
        byParameter.setServerName("apim.example");
        byParameter.setServerPort(443);
        byParameter.setScheme("https");

        underTest.doFilter(byParameter, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentType()).startsWith("text/xml");
        assertThat(response.getContentAsString())
            .contains("<wsdl:definitions", "soapAction=\"\"", "style=\"document\"")
            .contains("location=\"https://apim.example/v2/bulk-claims/services/BulkClaim\"")
            .doesNotContain("localhost:8080");
        assertThat(chain.getRequest()).isNull();

        response = new MockHttpServletResponse();
        underTest.doFilter(get(BulkClaimSoapFilter.WSDL_PATH), response, chain);
        assertThat(response.getContentAsString()).contains("location=\"http://localhost/v2/bulk-claims/services/BulkClaim\"");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Common.xsd", "RentalClaim.xsd", "MortgageClaim.xsd", "BulkClaimResponse.xsd",
        "BulkClaimStatusResponse.xsd", "BulkClaimStatusRequest.xsd", "BulkClaimStatusListResponse.xsd"})
    void shouldServeAllowedSchemaFiles(String fileName) throws Exception {
        underTest.doFilter(get(BulkClaimSoapFilter.SCHEMA_PATH + fileName), response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).contains("<xs:schema");
    }

    @ParameterizedTest
    @ValueSource(strings = {"../../application.yaml", "..%2F..%2Fapplication.yaml", "/etc/passwd", "rentalclaim.xsd",
        "RentalClaim.xsd/", "xsd/bulkclaim/v2/RentalClaim.xsd", "Other.xsd", ""})
    void shouldNotServeFilesOutsideTheAllowList(String fileName) throws Exception {
        underTest.doFilter(get(BulkClaimSoapFilter.SCHEMA_PATH + fileName), response, chain);

        assertThat(response.getStatus()).isEqualTo(404);
    }

    private static MockHttpServletRequest post(String body) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", BulkClaimSoapFilter.ENDPOINT_PATH);
        request.setContentType("text/xml");
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return request;
    }

    private static MockHttpServletRequest get(String path) {
        return new MockHttpServletRequest("GET", path);
    }
}
