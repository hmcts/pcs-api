package uk.gov.hmcts.reform.pcs.functional.tests;

import io.restassured.path.xml.XmlPath;
import lombok.extern.slf4j.Slf4j;
import io.restassured.response.Response;
import net.serenitybdd.annotations.Issue;
import net.serenitybdd.annotations.Title;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.hmcts.reform.pcs.functional.steps.BaseApi;
import uk.gov.hmcts.reform.pcs.functional.testutils.BulkClaimsTestOrganisation;
import uk.gov.hmcts.reform.pcs.functional.testutils.BulkClaimsV2Client;
import uk.gov.hmcts.reform.pcs.functional.testutils.PayloadLoader;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static uk.gov.hmcts.reform.pcs.functional.testutils.BulkClaimsV2Client.ENDPOINT_PATH;

@Slf4j
@Issue("HDPI-10171")
@Tag("Functional")
@ExtendWith(SerenityJUnit5Extension.class)
@EnabledIfEnvironmentVariable(named = "CCD_ENABLED", matches = "true")
@DisabledIfEnvironmentVariable(named = "SHUTTER_SERVICE", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BulkClaimsV2Tests extends BaseApi {

    private static final int STATUS_SUCCESS = 1;
    private static final int STATUS_FAILED = 2;
    private static final int FAIL_REASON_VALIDATION = 9;
    private static final int FAIL_REASON_IDENTIFIER_REUSED = 14;
    private static final int FAIL_REASON_CLAIM_NOT_FOUND = 17;
    private static final String SOCIAL_LANDLORD = "SocialLandlord";
    private static final String PRIVATE_LANDLORD = "PrivateLandlord";

    private final BulkClaimsV2Client client = new BulkClaimsV2Client();
    private String organisationId;
    private String pba;

    @BeforeAll
    void checkPrerequisites() {
        BulkClaimsTestOrganisation organisation = null;
        try {
            organisation = BulkClaimsTestOrganisation.resolve();
        } catch (BulkClaimsTestOrganisation.Unavailable e) {
            assumeTrue(false, "No bulk claims test organisation: " + e.getMessage()
                + ". Set BULK_CLAIMS_TEST_ORGANISATION_ID to choose one explicitly");
        }
        organisationId = organisation.organisationId();
        pba = organisation.pba();
        log.info("Bulk claims v2 tests use organisation {} with PBA {}", organisationId, pba);

        try {
            client.apimToken();
        } catch (RuntimeException e) {
            assumeTrue(false, "Cannot lease an S2S token for the APIM identity "
                + BulkClaimsV2Client.apimServiceName() + ": " + e.getMessage());
        }

        int wsdlStatus = client.wsdl(organisationId).statusCode();
        assumeTrue(wsdlStatus != 404, "Bulk claims v2 is not enabled in this environment");
    }

    @Title("The WSDL and schema endpoints serve known files and return 404 for an unknown schema file")
    @Test
    void wsdlAndSchemaEndpointsServeKnownFilesOnly() {
        Response wsdl = client.wsdl(organisationId);
        assertThat(wsdl.statusCode()).isEqualTo(200);
        assertThat(wsdl.asString()).contains("BulkClaimSoapBinding", "processRentalClaim", "getClaimStatuses");

        Response schema = client.schema("RentalClaim.xsd", organisationId);
        assertThat(schema.statusCode()).isEqualTo(200);
        assertThat(schema.asString()).contains("RentalClaimSet");

        assertThat(client.schema("Unknown.xsd", organisationId).statusCode()).isEqualTo(404);
    }

    @Title("validateOnly accepts a valid rental claim, rejects an invalid one and persists neither")
    @Test
    void validateOnlyReportsOutcomesWithoutPersisting() {
        String validCui = BulkClaimsV2Client.uniqueCustomerIdentifier();
        Response valid = client.submit("rental", rentalXml(validCui, SOCIAL_LANDLORD), organisationId, true);
        assertThat(valid.statusCode()).isEqualTo(200);
        XmlPath validBody = xml(valid);
        assertThat(validBody.getInt("BulkClaimResponse.ClaimResponse.ClaimStatus.StatusCode"))
            .isEqualTo(STATUS_SUCCESS);
        assertThat(validBody.getString("BulkClaimResponse.ClaimResponse.CaseReference")).isEmpty();

        String invalidCui = BulkClaimsV2Client.uniqueCustomerIdentifier();
        Response invalid = client.submit("rental", invalidRentalXml(invalidCui), organisationId, true);
        assertThat(invalid.statusCode()).isEqualTo(200);
        XmlPath invalidBody = xml(invalid);
        assertThat(invalidBody.getInt("BulkClaimResponse.ClaimResponse.ClaimStatus.StatusCode"))
            .isEqualTo(STATUS_FAILED);
        assertThat(invalidBody.getInt("BulkClaimResponse.ClaimResponse.ClaimStatus.FailReasonCode"))
            .isEqualTo(FAIL_REASON_VALIDATION);
        assertThat(invalidBody.getList("BulkClaimResponse.ClaimResponse.ValidationErrors.ValidationError"))
            .isNotEmpty();

        assertThat(notFound(client.status(validCui, organisationId))).isTrue();
        assertThat(notFound(client.status(invalidCui, organisationId))).isTrue();
    }

    @Title("A valid England social landlord rental claim creates a case, reports its status and is idempotent")
    @Test
    void validRentalClaimCreatesCaseAndResubmissionReturnsSameCase() {
        String cui = BulkClaimsV2Client.uniqueCustomerIdentifier();
        String claimXml = rentalXml(cui, SOCIAL_LANDLORD);

        String caseReference = submitAndGetCaseReference("rental", claimXml, cui);

        Response status = client.status(cui, organisationId);
        assertThat(status.statusCode()).isEqualTo(200);
        XmlPath statusBody = xml(status);
        assertThat(statusBody.getInt("BulkClaimStatusResponse.ClaimStatus.StatusCode")).isEqualTo(STATUS_SUCCESS);
        assertThat(statusBody.getString("BulkClaimStatusResponse.CaseReference")).isEqualTo(caseReference);
        assertThat(statusBody.getString("BulkClaimStatusResponse.CaseState"))
            .isIn("PENDING_CASE_ISSUED", "CASE_ISSUED");
        assertThat(statusBody.getString("BulkClaimStatusResponse.PaymentStatus")).isNotBlank();

        assertThat(submitAndGetCaseReference("rental", claimXml, cui)).isEqualTo(caseReference);
    }

    @Title("Batch status lists a submitted claim and reports an unknown identifier as not found, in request order")
    @Test
    void batchStatusReportsKnownAndUnknownIdentifiers() {
        String cui = BulkClaimsV2Client.uniqueCustomerIdentifier();
        String unknown = BulkClaimsV2Client.uniqueCustomerIdentifier();
        String caseReference = submitAndGetCaseReference("rental", rentalXml(cui, SOCIAL_LANDLORD), cui);

        Response response = client.statusBatch(statusRequest(unknown, cui), organisationId);

        assertThat(response.statusCode()).as(response.asString()).isEqualTo(200);
        XmlPath body = xml(response);
        assertThat(body.getList("BulkClaimStatusListResponse.Claim")).hasSize(2);
        assertThat(body.getString("BulkClaimStatusListResponse.Claim[0].CustomerUniqueIdentifier"))
            .isEqualTo(unknown);
        assertThat(body.getInt("BulkClaimStatusListResponse.Claim[0].ClaimStatus.StatusCode"))
            .isEqualTo(STATUS_FAILED);
        assertThat(body.getInt("BulkClaimStatusListResponse.Claim[0].ClaimStatus.FailReasonCode"))
            .isEqualTo(FAIL_REASON_CLAIM_NOT_FOUND);
        assertThat(body.getString("BulkClaimStatusListResponse.Claim[1].CustomerUniqueIdentifier")).isEqualTo(cui);
        assertThat(body.getInt("BulkClaimStatusListResponse.Claim[1].ClaimStatus.StatusCode"))
            .isEqualTo(STATUS_SUCCESS);
        assertThat(body.getString("BulkClaimStatusListResponse.Claim[1].CaseReference")).isEqualTo(caseReference);
    }

    @Title("Reusing a customer unique identifier for a changed claim fails with reason 14")
    @Test
    void reusedIdentifierForChangedClaimIsRejected() {
        String cui = BulkClaimsV2Client.uniqueCustomerIdentifier();
        submitAndGetCaseReference("rental", rentalXml(cui, SOCIAL_LANDLORD), cui);

        String changed = PayloadLoader.load("/payloads/bulkclaims/rental-england-valid.xml", Map.of(
            "pba", pba,
            "customerUniqueIdentifier", cui,
            "addressLine1", "11 Changed Road",
            "claimantDesignation", SOCIAL_LANDLORD
        ));
        Response response = client.submit("rental", changed, organisationId, false);

        assertThat(response.statusCode()).isEqualTo(200);
        XmlPath body = xml(response);
        assertThat(body.getInt("BulkClaimResponse.ClaimResponse.ClaimStatus.StatusCode")).isEqualTo(STATUS_FAILED);
        assertThat(body.getInt("BulkClaimResponse.ClaimResponse.ClaimStatus.FailReasonCode"))
            .isEqualTo(FAIL_REASON_IDENTIFIER_REUSED);
    }

    @Title("A private landlord England rental claim is accepted")
    @Test
    void privateLandlordRentalClaimIsAccepted() {
        String cui = BulkClaimsV2Client.uniqueCustomerIdentifier();
        submitAndGetCaseReference("rental", rentalXml(cui, PRIVATE_LANDLORD), cui);
    }

    @Title("Requests from a service that is not the APIM identity are forbidden")
    @Test
    void nonApimServiceCannotUseBulkClaimsApi() {
        Response response = client.request(client.nonApimToken())
            .header(BulkClaimsV2Client.ORGANISATION_HEADER, organisationId)
            .contentType(BulkClaimsV2Client.SOAP_CONTENT_TYPE)
            .body(validateOnlyRentalEnvelope())
            .post(ENDPOINT_PATH);

        assertThat(response.statusCode()).isEqualTo(403);
    }

    @Title("The APIM identity is forbidden from non-bulk endpoints")
    @Test
    void apimIdentityCannotCallOtherEndpoints() {
        Response response = client.request(client.apimToken())
            .header(BulkClaimsV2Client.ORGANISATION_HEADER, organisationId)
            .get("/payment/pba-accounts");

        assertThat(response.statusCode()).isEqualTo(403);
    }

    @Title("A bulk claims request without an organisation id is forbidden")
    @Test
    void missingOrganisationHeaderIsForbidden() {
        Response response = client.request(client.apimToken())
            .contentType(BulkClaimsV2Client.SOAP_CONTENT_TYPE)
            .body(validateOnlyRentalEnvelope())
            .post(ENDPOINT_PATH);

        assertThat(response.statusCode()).isEqualTo(403);
    }

    @Title("The mortgage operation is a SOAP fault when its flag is off, otherwise accepts a valid mortgage claim")
    @Test
    void mortgageEndpointHonoursItsFeatureFlag() {
        String cui = BulkClaimsV2Client.uniqueCustomerIdentifier();
        String claimXml = PayloadLoader.load("/payloads/bulkclaims/mortgage-england-valid.xml", Map.of(
            "pba", pba,
            "customerUniqueIdentifier", cui
        ));

        Response response = client.submit("mortgage", claimXml, organisationId, false);

        if (response.statusCode() == 500 && response.asString().contains("is not available")) {
            return;
        }
        assertThat(response.statusCode()).isEqualTo(200);
        XmlPath body = xml(response);
        assertThat(body.getInt("BulkClaimResponse.ClaimResponse.ClaimStatus.StatusCode")).isEqualTo(STATUS_SUCCESS);
        assertThat(body.getString("BulkClaimResponse.ClaimResponse.CaseReference")).matches("\\d{16}");
    }

    private String submitAndGetCaseReference(String kind, String claimXml, String cui) {
        Response response = client.submit(kind, claimXml, organisationId, false);

        assertThat(response.statusCode()).as(response.asString()).isEqualTo(200);
        XmlPath body = xml(response);
        assertThat(body.getInt("BulkClaimResponse.BulkStatus.StatusCode")).as(response.asString())
            .isEqualTo(STATUS_SUCCESS);
        assertThat(body.getString("BulkClaimResponse.ClaimResponse.CustomerUniqueIdentifier")).isEqualTo(cui);
        assertThat(body.getInt("BulkClaimResponse.ClaimResponse.ClaimStatus.StatusCode"))
            .as(response.asString()).isEqualTo(STATUS_SUCCESS);
        String caseReference = body.getString("BulkClaimResponse.ClaimResponse.CaseReference");
        assertThat(caseReference).matches("\\d{16}");
        return caseReference;
    }

    private String rentalXml(String cui, String claimantDesignation) {
        return PayloadLoader.load("/payloads/bulkclaims/rental-england-valid.xml", Map.of(
            "pba", pba,
            "customerUniqueIdentifier", cui,
            "addressLine1", "10 Rented Road",
            "claimantDesignation", claimantDesignation
        ));
    }

    private String invalidRentalXml(String cui) {
        return PayloadLoader.load("/payloads/bulkclaims/rental-england-invalid.xml", Map.of(
            "pba", pba,
            "customerUniqueIdentifier", cui
        ));
    }

    private static String statusRequest(String... identifiers) {
        return "<BulkClaimStatusRequest xmlns=\"http://hmcts.gov.uk/pcs/bulkclaim/v2\">"
            + Arrays.stream(identifiers)
                .map(identifier -> "<CustomerUniqueIdentifier>" + identifier + "</CustomerUniqueIdentifier>")
                .collect(Collectors.joining())
            + "</BulkClaimStatusRequest>";
    }

    private String validateOnlyRentalEnvelope() {
        return BulkClaimsV2Client.soapEnvelope("processRentalClaim", "rentalRequest",
            rentalXml(BulkClaimsV2Client.uniqueCustomerIdentifier(), SOCIAL_LANDLORD), true);
    }

    private static boolean notFound(Response status) {
        return status.statusCode() == 200
            && xml(status).getInt("BulkClaimStatusResponse.ClaimStatus.FailReasonCode") == FAIL_REASON_CLAIM_NOT_FOUND;
    }

    private static XmlPath xml(Response response) {
        return XmlPath.from(BulkClaimsV2Client.returnDocument(response));
    }
}
