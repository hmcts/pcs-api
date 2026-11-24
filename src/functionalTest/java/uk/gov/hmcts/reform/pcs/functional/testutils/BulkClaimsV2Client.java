package uk.gov.hmcts.reform.pcs.functional.testutils;

import io.restassured.path.xml.XmlPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import net.serenitybdd.rest.SerenityRest;
import uk.gov.hmcts.reform.pcs.functional.config.TestConstants;

import java.util.Optional;
import java.util.UUID;

/**
 * Calls the bulk claims v2 API the way APIM does: with the S2S token of the APIM service identity and the
 * organisation id in a header.
 */
public class BulkClaimsV2Client {

    public static final String BASE_PATH = "/v2/bulk-claims/services";
    public static final String ENDPOINT_PATH = BASE_PATH + "/BulkClaim";
    public static final String WSDL_PATH = BASE_PATH + "/BulkClaim.wsdl";
    public static final String SOAP_NAMESPACE = "http://impl.service.bulkclaim.webservice.pcol.dca.gov.uk";
    public static final String SOAP_CONTENT_TYPE = "text/xml;charset=UTF-8";
    public static final String ORGANISATION_HEADER = "X-Organisation-Id";

    private static final String DEFAULT_APIM_SERVICE = "pcs_bulk_claims_apim";
    private static final String DEFAULT_PBA = "PBA0084541";

    private final String baseUrl = System.getenv("TEST_URL");
    private final ServiceAuthenticationGenerator s2s = new ServiceAuthenticationGenerator();
    private String apimToken;

    public static String apimServiceName() {
        return envOrDefault("BULK_CLAIMS_APIM_SERVICE_NAME", DEFAULT_APIM_SERVICE);
    }

    public static Optional<String> organisationId() {
        return Optional.ofNullable(System.getenv("BULK_CLAIMS_TEST_ORGANISATION_ID")).filter(v -> !v.isBlank());
    }

    public static String pbaAccount() {
        return envOrDefault("BULK_CLAIMS_TEST_PBA", DEFAULT_PBA);
    }

    public static String uniqueCustomerIdentifier() {
        return "FT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }

    /** Leases an S2S token for the APIM identity; throws when that identity is not registered in S2S. */
    public String apimToken() {
        if (apimToken == null) {
            apimToken = s2s.generate(apimServiceName());
        }
        return apimToken;
    }

    public String nonApimToken() {
        return s2s.generate(TestConstants.PCS_FRONTEND);
    }

    public RequestSpecification apimRequest(String organisationId) {
        return request(apimToken()).header(ORGANISATION_HEADER, organisationId);
    }

    public RequestSpecification request(String serviceToken) {
        return SerenityRest.given()
            .baseUri(baseUrl)
            .relaxedHTTPSValidation()
            .header(TestConstants.SERVICE_AUTHORIZATION, serviceToken);
    }

    public Response submit(String kind, String xml, String organisationId, boolean validateOnly) {
        boolean mortgage = "mortgage".equals(kind);
        return apimRequest(organisationId)
            .contentType(SOAP_CONTENT_TYPE)
            .body(soapEnvelope(mortgage ? "processMortgageClaim" : "processRentalClaim",
                               mortgage ? "mortgageRequest" : "rentalRequest", xml, validateOnly))
            .post(ENDPOINT_PATH);
    }

    public Response status(String customerUniqueIdentifier, String organisationId) {
        return apimRequest(organisationId)
            .contentType(SOAP_CONTENT_TYPE)
            .body(soapEnvelope("getClaimStatus", "customerUniqueIdentifier", customerUniqueIdentifier, null))
            .post(ENDPOINT_PATH);
    }

    public Response statusBatch(String xml, String organisationId) {
        return apimRequest(organisationId)
            .contentType(SOAP_CONTENT_TYPE)
            .body(soapEnvelope("getClaimStatuses", "statusRequest", xml, null))
            .post(ENDPOINT_PATH);
    }

    public Response wsdl(String organisationId) {
        return apimRequest(organisationId).get(WSDL_PATH);
    }

    public Response schema(String fileName, String organisationId) {
        return apimRequest(organisationId).get(BASE_PATH + "/schema/" + fileName);
    }

    /** The SOAP 1.1 request an Axis client generated from PCOL's WSDL sends, carrying the v2 XML as a string. */
    public static String soapEnvelope(String operation, String child, String value, Boolean validateOnly) {
        return "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body>"
            + "<ns1:" + operation + " xmlns:ns1=\"" + SOAP_NAMESPACE + "\"><ns1:" + child + ">"
            + value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") + "</ns1:" + child + ">"
            + (validateOnly == null ? "" : "<ns1:validateOnly>" + validateOnly + "</ns1:validateOnly>")
            + "</ns1:" + operation + "></soapenv:Body></soapenv:Envelope>";
    }

    /** The v2 XML document carried in the return string of a SOAP response. */
    public static String returnDocument(Response response) {
        return XmlPath.from(response.asString()).getString("**.find { it.name().endsWith('Return') }");
    }

    private static String envOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
