package uk.gov.hmcts.reform.pcs.functional.testutils;

import io.restassured.response.Response;
import net.serenitybdd.rest.SerenityRest;
import uk.gov.hmcts.reform.pcs.functional.config.TestConstants;

import java.util.List;
import java.util.Optional;

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static uk.gov.hmcts.reform.pcs.functional.config.AuthConfig.CLIENT_ID;
import static uk.gov.hmcts.reform.pcs.functional.config.AuthConfig.ENDPOINT;
import static uk.gov.hmcts.reform.pcs.functional.config.AuthConfig.GRANT_TYPE;
import static uk.gov.hmcts.reform.pcs.functional.config.AuthConfig.SCOPE;

/**
 * The organisation and PBA account the bulk claims v2 tests submit on behalf of. Taken from
 * BULK_CLAIMS_TEST_ORGANISATION_ID when set, otherwise resolved from rd-professional as the organisation of the
 * PCS automation solicitor.
 */
public record BulkClaimsTestOrganisation(String organisationId, String pba) {

    private static final String DEFAULT_PRD_URL = "http://rd-professional-api-aat.service.core-compute-aat.internal";
    private static final String ACTIVE = "ACTIVE";
    private static final String SOLICITOR_PROFILE = "SOLICITOR_PROFILE";

    /** Thrown when no usable organisation can be found; the message says why. */
    public static class Unavailable extends RuntimeException {
        Unavailable(String message) {
            super(message);
        }
    }

    public static BulkClaimsTestOrganisation resolve() {
        Optional<String> configured = BulkClaimsV2Client.organisationId();
        if (configured.isPresent()) {
            return new BulkClaimsTestOrganisation(configured.get(), BulkClaimsV2Client.pbaAccount());
        }
        return lookUpSolicitorOrganisation();
    }

    private static BulkClaimsTestOrganisation lookUpSolicitorOrganisation() {
        String uid = require("PCS_SOLICITOR_AUTOMATION_UID");
        String idamUrl = require("IDAM_API_URL");
        String clientSecret = require("PCS_API_IDAM_SECRET");
        String prdAdminUsername = require("PCS_PRD_ADMIN_USERNAME");
        String prdAdminPassword = require("PCS_PRD_ADMIN_PASSWORD");
        String prdUrl = Optional.ofNullable(System.getenv("RD_PROFESSIONAL_API_URL"))
            .filter(v -> !v.isBlank())
            .orElse(DEFAULT_PRD_URL);

        try {
            String idamToken = prdAdminToken(idamUrl, clientSecret, prdAdminUsername, prdAdminPassword);
            String s2sToken = new ServiceAuthenticationGenerator().generate(TestConstants.PCS_API);

            Response response = SerenityRest.given()
                .relaxedHTTPSValidation()
                .baseUri(prdUrl)
                .header(TestConstants.AUTHORIZATION, "Bearer " + idamToken)
                .header(TestConstants.SERVICE_AUTHORIZATION, s2sToken)
                .get("/refdata/internal/v1/organisations/orgDetails/" + uid);
            if (response.statusCode() != 200) {
                throw new Unavailable("rd-professional returned " + response.statusCode()
                    + " for the organisation of " + TestConstants.PCS_SOLICITOR_USER);
            }

            String organisationId = response.jsonPath().getString("organisationIdentifier");
            String status = response.jsonPath().getString("status");
            List<String> profiles = response.jsonPath().getList("organisationProfileIds", String.class);
            final List<String> paymentAccounts = response.jsonPath().getList("paymentAccount", String.class);

            if (organisationId == null || organisationId.isBlank()) {
                throw new Unavailable("rd-professional returned no organisationIdentifier");
            }
            if (!ACTIVE.equalsIgnoreCase(status)) {
                throw new Unavailable("Organisation " + organisationId + " is " + status + ", not " + ACTIVE);
            }

            if (profiles == null || !profiles.contains(SOLICITOR_PROFILE)) {
                throw new Unavailable("Organisation " + organisationId + " is not a solicitor organisation (profiles: "
                    + profiles + "); bulk claims require a solicitor firm");
            }

            String pba = Optional.ofNullable(System.getenv("BULK_CLAIMS_TEST_PBA"))
                .filter(v -> !v.isBlank())
                .or(() -> Optional.ofNullable(paymentAccounts).stream().flatMap(List::stream).findFirst())
                .orElseThrow(() -> new Unavailable("Organisation " + organisationId + " has no PBA account"));

            return new BulkClaimsTestOrganisation(organisationId, pba);
        } catch (Unavailable e) {
            throw e;
        } catch (RuntimeException e) {
            throw new Unavailable("Could not look up the organisation of " + TestConstants.PCS_SOLICITOR_USER
                + ": " + e.getMessage());
        }
    }

    private static String prdAdminToken(String idamUrl, String clientSecret, String username, String password) {
        Response response = SerenityRest.given()
            .baseUri(idamUrl)
            .contentType(APPLICATION_FORM_URLENCODED_VALUE)
            .formParam("username", username)
            .formParam("password", password)
            .formParam("client_id", CLIENT_ID)
            .formParam("client_secret", clientSecret)
            .formParam("scope", SCOPE)
            .formParam("grant_type", GRANT_TYPE)
            .post(ENDPOINT);
        if (response.statusCode() != 200) {
            throw new Unavailable("IDAM returned " + response.statusCode() + " for the prd-admin token");
        }
        return response.jsonPath().getString("access_token");
    }

    private static String require(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new Unavailable(name + " is not set");
        }
        return value;
    }
}
