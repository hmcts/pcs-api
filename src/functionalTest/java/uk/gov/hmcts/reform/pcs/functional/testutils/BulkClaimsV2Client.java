package uk.gov.hmcts.reform.pcs.functional.testutils;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import net.serenitybdd.rest.SerenityRest;
import uk.gov.hmcts.reform.pcs.functional.config.TestConstants;

import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;

/**
 * Calls the bulk claims v2 API the way APIM does: with the S2S token of the APIM service identity and the
 * organisation id in a header.
 */
public class BulkClaimsV2Client {

    public static final String BASE_PATH = "/v2/bulk-claims";
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
        return apimRequest(organisationId)
            .contentType(APPLICATION_XML_VALUE)
            .queryParam("validateOnly", validateOnly)
            .body(xml)
            .post(BASE_PATH + "/" + kind);
    }

    public Response status(String customerUniqueIdentifier, String organisationId) {
        return apimRequest(organisationId).get(BASE_PATH + "/claims/" + customerUniqueIdentifier);
    }

    public Response schema(String fileName, String organisationId) {
        return apimRequest(organisationId).get(BASE_PATH + "/schema/" + fileName);
    }

    private static String envOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
