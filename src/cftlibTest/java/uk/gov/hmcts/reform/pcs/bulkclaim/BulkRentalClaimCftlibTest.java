package uk.gov.hmcts.reform.pcs.bulkclaim;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType;
import uk.gov.hmcts.reform.pcs.model.FeePaymentSummary;
import uk.gov.hmcts.reform.pcs.service.FeePaymentService;
import uk.gov.hmcts.rse.ccd.lib.test.CftlibTest;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.auth.ServiceAuthorizationGenerator.generateTestS2SToken;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
class BulkRentalClaimCftlibTest extends CftlibTest {

    private static final String PCS_API = "http://localhost:3206";
    private static final String SHARED_WIREMOCK = "http://localhost:8083";
    private static final String APIM_SERVICE = "pcs_bulk_claims_apim";
    private static final String ORGANISATION_ID = "BULKORG1";
    private static final String PBA_ACCOUNT = "PBA1234567";
    private static final Path FIXTURES = Path.of("src/test/resources/bulkclaim/v2");
    private static final Pattern CLAIM_ELEMENT =
        Pattern.compile("<RentalClaim>.*?</RentalClaim>", Pattern.DOTALL);

    private record Response(int status, String body) {
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private FeePaymentService feePaymentService;
    @Autowired
    private ObjectMapper objectMapper;

    private final RestClient restClient = RestClient.create(PCS_API);

    private String validCui;
    private String invalidCui;
    private String requestXml;
    private long caseReference;
    private String serviceRequestReference;

    @BeforeAll
    void setup() throws IOException {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        validCui = "VALID-" + suffix;
        invalidCui = "INVALID-" + suffix;

        String validClaim = claimElement("rental-england-assured-arrears-valid.xml")
            .replace("ASSURED-ARREARS", validCui)
            .replace("M1 1AA", "BD10 0JB");
        String invalidClaim = claimElement("rental-england-invalid-combinations.xml")
            .replace("INVALID-1", invalidCui)
            .replace("M1 1AA", "BD10 0JB");

        requestXml = claimSet(validClaim, invalidClaim, validClaim);
    }

    @Test
    @Order(1)
    void createsOnlyTheValidClaimAndReportsPerClaimOutcomes() throws Exception {
        Response response = postRental(requestXml, APIM_SERVICE, ORGANISATION_ID);

        assertThat(response.status()).isEqualTo(200);
        Document xml = parse(response.body());

        assertThat(text(xml, "/BulkClaimResponse/BulkStatus/FailReasonCode")).isEqualTo("4");

        List<Node> claims = nodes(xml, "/BulkClaimResponse/ClaimResponse");
        assertThat(claims).hasSize(3);

        Node valid = claims.get(0);
        assertThat(text(valid, "CustomerUniqueIdentifier")).isEqualTo(validCui);
        assertThat(text(valid, "ClaimStatus/StatusCode")).isEqualTo("1");
        caseReference = Long.parseLong(text(valid, "CaseReference"));

        Node invalid = claims.get(1);
        assertThat(text(invalid, "CustomerUniqueIdentifier")).isEqualTo(invalidCui);
        assertThat(text(invalid, "ClaimStatus/StatusCode")).isEqualTo("2");
        assertThat(text(invalid, "ClaimStatus/FailReasonCode")).isEqualTo("9");
        assertThat(nodes(invalid, "ValidationErrors/ValidationError")).isNotEmpty();

        Node duplicate = claims.get(2);
        assertThat(text(duplicate, "CustomerUniqueIdentifier")).isEqualTo(validCui);
        assertThat(text(duplicate, "ClaimStatus/StatusCode")).isEqualTo("2");
        assertThat(text(duplicate, "ClaimStatus/FailReasonCode")).isEqualTo("14");
        assertThat(text(duplicate, "ValidationErrors/ValidationError/Code")).isEqualTo("DUPLICATE_IDENTIFIER");

        assertThat(caseState(caseReference)).isEqualTo(State.PENDING_CASE_ISSUED.name());

        Map<String, Object> submission = submissionRow(validCui);
        assertThat(submission.get("status")).isEqualTo("CASE_CREATED");
        assertThat(submission.get("organisation_id")).isEqualTo(ORGANISATION_ID);
        assertThat(((Number) submission.get("case_reference")).longValue()).isEqualTo(caseReference);

        assertThat(submissionRow(invalidCui).get("status")).isEqualTo("FAILED_VALIDATION");

        assertThat(jdbcTemplate.queryForList(
            """
                SELECT c.submission_channel
                FROM claim c JOIN pcs_case pc ON pc.id = c.case_id
                WHERE pc.case_reference = ?
                """, String.class, caseReference))
            .containsExactly("BULK_API");

        assertThat(jdbcTemplate.queryForList(
            """
                SELECT p.organisation_id || '|' || p.organisation_profile_id
                FROM party p JOIN pcs_case pc ON pc.id = p.case_id
                WHERE pc.case_reference = ? AND p.organisation_id IS NOT NULL
                """, String.class, caseReference))
            .contains(ORGANISATION_ID + "|SOLICITOR_PROFILE");
    }

    @Test
    @Order(2)
    void revokesTheCreatorRoleFromTheSystemUser() {
        Awaitility.await("Creator role revoke task completed for case " + caseReference)
            .atMost(Duration.ofSeconds(60))
            .pollInterval(Duration.ofSeconds(1))
            .until(() -> pendingTasks("role-assignment-task", caseReference) == 0);
    }

    @Test
    @Order(3)
    void paysTheIssueFeeFromTheOrganisationsPbaAccount() throws Exception {
        List<FeePaymentSummary> payments =
            feePaymentService.waitForFeePaymentRequests(caseReference, PaymentCallbackHandlerType.CLAIM);
        assertThat(payments).hasSize(1);
        serviceRequestReference = payments.getFirst().getServiceRequestReference();
        assertThat(serviceRequestReference).isNotBlank();

        JsonNode pbaRequest = awaitPbaPaymentRequest(serviceRequestReference);
        assertThat(pbaRequest.path("account_number").asText()).isEqualTo(PBA_ACCOUNT);
        assertThat(pbaRequest.path("amount").decimalValue())
            .isEqualByComparingTo(payments.getFirst().getAmount());
        assertThat(pbaRequest.path("idempotency_key").asText())
            .startsWith(serviceRequestReference + "-")
            .matches(".+-[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}");
        assertThat(pbaRequest.path("organisation_name").asText()).isEqualTo("Bulk Housing Association");

        Awaitility.await("Submission records the PBA payment as submitted")
            .atMost(Duration.ofSeconds(30))
            .pollInterval(Duration.ofSeconds(1))
            .until(() -> {
                Map<String, Object> row = submissionRow(validCui);
                return serviceRequestReference.equals(row.get("service_request_reference"))
                    && !"NOT_STARTED".equals(row.get("payment_status"));
            });
    }

    @Test
    @Order(4)
    void issuesTheCaseOncePaymentIsConfirmed() throws Exception {
        List<FeePaymentSummary> payments =
            feePaymentService.waitForFeePaymentRequests(caseReference, PaymentCallbackHandlerType.CLAIM);
        feePaymentService.simulatePayments(caseReference, payments);

        Awaitility.await("Case " + caseReference + " issued")
            .atMost(Duration.ofSeconds(60))
            .pollInterval(Duration.ofSeconds(1))
            .until(() -> State.CASE_ISSUED.name().equals(caseState(caseReference)));

        Awaitility.await("Claim form generation completed for case " + caseReference)
            .atMost(Duration.ofSeconds(60))
            .pollInterval(Duration.ofSeconds(1))
            .until(() -> pendingTasks("claim-form-generation-task", caseReference) == 0);

        Response status = get("/v2/bulk-claims/claims/" + validCui, APIM_SERVICE, ORGANISATION_ID);
        assertThat(status.status()).isEqualTo(200);
        Document xml = parse(status.body());
        assertThat(text(xml, "/BulkClaimStatusResponse/CaseReference")).isEqualTo(String.valueOf(caseReference));
        assertThat(text(xml, "/BulkClaimStatusResponse/CaseState")).isEqualTo(State.CASE_ISSUED.name());
        assertThat(text(xml, "/BulkClaimStatusResponse/PaymentStatus")).isEqualTo("PAID");
        assertThat(text(xml, "/BulkClaimStatusResponse/ServiceRequestReference"))
            .isEqualTo(serviceRequestReference);

        Response batch = postStatusBatch(statusRequest(validCui, "NO-SUCH-CLAIM"), APIM_SERVICE, ORGANISATION_ID);
        assertThat(batch.status()).isEqualTo(200);
        List<Node> entries = nodes(parse(batch.body()), "/BulkClaimStatusListResponse/Claim");
        assertThat(entries).hasSize(2);
        assertThat(text(entries.get(0), "CustomerUniqueIdentifier")).isEqualTo(validCui);
        assertThat(text(entries.get(0), "CaseReference")).isEqualTo(String.valueOf(caseReference));
        assertThat(text(entries.get(0), "CaseState")).isEqualTo(State.CASE_ISSUED.name());
        assertThat(text(entries.get(0), "PaymentStatus")).isEqualTo("PAID");
        assertThat(text(entries.get(1), "CustomerUniqueIdentifier")).isEqualTo("NO-SUCH-CLAIM");
        assertThat(text(entries.get(1), "ClaimStatus/StatusCode")).isEqualTo("2");
        assertThat(text(entries.get(1), "ClaimStatus/FailReasonCode")).isEqualTo("17");
    }

    @Test
    @Order(5)
    void resubmittingTheSameRequestRepeatsTheOutcomesWithoutCreatingCases() throws Exception {
        int casesBefore = countSubmissionsWithCase();

        Response response = postRental(requestXml, APIM_SERVICE, ORGANISATION_ID);

        assertThat(response.status()).isEqualTo(200);
        List<Node> claims = nodes(parse(response.body()), "/BulkClaimResponse/ClaimResponse");
        assertThat(claims).hasSize(3);
        assertThat(text(claims.get(0), "ClaimStatus/StatusCode")).isEqualTo("1");
        assertThat(text(claims.get(0), "CaseReference")).isEqualTo(String.valueOf(caseReference));
        assertThat(text(claims.get(1), "ClaimStatus/FailReasonCode")).isEqualTo("9");
        assertThat(text(claims.get(2), "ClaimStatus/FailReasonCode")).isEqualTo("14");
        assertThat(countSubmissionsWithCase()).isEqualTo(casesBefore);
    }

    @Test
    @Order(6)
    void reusingAnIdentifierForAChangedClaimIsRejected() throws Exception {
        String changed = claimElement("rental-england-assured-arrears-valid.xml")
            .replace("ASSURED-ARREARS", validCui)
            .replace("M1 1AA", "BD10 0JB")
            .replace("Example Housing Association", "Another Housing Association");

        Response response = postRental(claimSet(changed), APIM_SERVICE, ORGANISATION_ID);

        assertThat(response.status()).isEqualTo(200);
        Node claim = nodes(parse(response.body()), "/BulkClaimResponse/ClaimResponse").getFirst();
        assertThat(text(claim, "ClaimStatus/FailReasonCode")).isEqualTo("14");
        assertThat(submissionRow(validCui).get("status")).isEqualTo("CASE_CREATED");
    }

    @Test
    @Order(7)
    void rejectsCallersOutsideTheApimIdentity() {
        assertThat(postRental(requestXml, "xui_webapp", ORGANISATION_ID).status()).isEqualTo(403);
        assertThat(postRental(requestXml, APIM_SERVICE, null).status()).isEqualTo(403);
        assertThat(postRental(requestXml, APIM_SERVICE, "  ").status()).isEqualTo(403);

        assertThat(get("/v2/bulk-claims/claims/" + validCui, "xui_webapp", ORGANISATION_ID).status())
            .isEqualTo(403);
    }

    @Test
    @Order(8)
    void apimIdentityCannotReachAnythingOutsideBulkClaims() {
        assertThat(get("/testing-support/fee-payment-info/" + caseReference, APIM_SERVICE, ORGANISATION_ID).status())
            .isEqualTo(403);
        assertThat(get("/ccd-persistence/cases/" + caseReference, APIM_SERVICE, ORGANISATION_ID).status())
            .isEqualTo(403);

        int status = restClient.put()
            .uri("/payment-update")
            .contentType(MediaType.APPLICATION_JSON)
            .header("ServiceAuthorization", generateTestS2SToken(APIM_SERVICE))
            .body("{}")
            .exchange((request, response) -> response.getStatusCode().value());
        assertThat(status).isEqualTo(403);
    }

    @Test
    @Order(9)
    void servesTheSchemaAndReportsUnknownClaims() throws Exception {
        Response schema = get("/v2/bulk-claims/schema/RentalClaim.xsd", APIM_SERVICE, ORGANISATION_ID);
        assertThat(schema.status()).isEqualTo(200);
        assertThat(schema.body()).contains("RentalClaimSet");

        assertThat(get("/v2/bulk-claims/schema/Unknown.xsd", APIM_SERVICE, ORGANISATION_ID).status())
            .isEqualTo(404);
        assertThat(get("/v2/bulk-claims/claims/NO-SUCH-" + UUID.randomUUID(), APIM_SERVICE, ORGANISATION_ID)
                       .status()).isEqualTo(404);
        assertThat(get("/v2/bulk-claims/claims/" + validCui, APIM_SERVICE, "OTHERORG").status())
            .isEqualTo(404);
    }

    private Response postRental(String body, String serviceName, String organisationId) {
        return restClient.post()
            .uri("/v2/bulk-claims/rental")
            .contentType(MediaType.APPLICATION_XML)
            .headers(headers -> applyHeaders(headers, serviceName, organisationId))
            .body(body.getBytes(StandardCharsets.UTF_8))
            .exchange((request, response) -> new Response(
                response.getStatusCode().value(),
                new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8)));
    }

    private Response postStatusBatch(String body, String serviceName, String organisationId) {
        return restClient.post()
            .uri("/v2/bulk-claims/claims/status")
            .contentType(MediaType.APPLICATION_XML)
            .headers(headers -> applyHeaders(headers, serviceName, organisationId))
            .body(body.getBytes(StandardCharsets.UTF_8))
            .exchange((request, response) -> new Response(
                response.getStatusCode().value(),
                new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8)));
    }

    private static String statusRequest(String... identifiers) {
        return "<BulkClaimStatusRequest xmlns=\"http://hmcts.gov.uk/pcs/bulkclaim/v2\">"
            + Arrays.stream(identifiers)
                .map(identifier -> "<CustomerUniqueIdentifier>" + identifier + "</CustomerUniqueIdentifier>")
                .collect(Collectors.joining())
            + "</BulkClaimStatusRequest>";
    }

    private Response get(String path, String serviceName, String organisationId) {
        return restClient.get()
            .uri(path)
            .headers(headers -> applyHeaders(headers, serviceName, organisationId))
            .exchange((request, response) -> new Response(
                response.getStatusCode().value(),
                new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8)));
    }

    private static void applyHeaders(HttpHeaders headers,
                                     String serviceName,
                                     String organisationId) {
        headers.set("ServiceAuthorization", generateTestS2SToken(serviceName));
        if (organisationId != null) {
            headers.set("X-Organisation-Id", organisationId);
        }
    }

    private JsonNode awaitPbaPaymentRequest(String serviceRequestReference) {
        String path = "/payments/service-request/" + serviceRequestReference + "/pba-payments";
        return Awaitility.await("PBA payment request sent for " + serviceRequestReference)
            .atMost(Duration.ofSeconds(60))
            .pollInterval(Duration.ofSeconds(1))
            .until(() -> findWiremockRequest(path), Objects::nonNull);
    }

    private JsonNode findWiremockRequest(String path) throws IOException {
        String journal = RestClient.create(SHARED_WIREMOCK).get().uri("/__admin/requests").retrieve()
            .body(String.class);
        for (JsonNode entry : objectMapper.readTree(journal).path("requests")) {
            JsonNode request = entry.path("request");
            if ("POST".equals(request.path("method").asText()) && path.equals(request.path("url").asText())) {
                return objectMapper.readTree(request.path("body").asText());
            }
        }
        return null;
    }

    private String caseState(long reference) {
        return jdbcTemplate.queryForObject("SELECT state FROM ccd.case_data WHERE reference = ?",
                                           String.class, reference);
    }

    private Map<String, Object> submissionRow(String cui) {
        return jdbcTemplate.queryForMap(
            "SELECT * FROM bulk_claim_submission WHERE organisation_id = ? AND customer_unique_identifier = ?",
            ORGANISATION_ID, cui);
    }

    private int countSubmissionsWithCase() {
        return jdbcTemplate.queryForObject(
            "SELECT count(*) FROM bulk_claim_submission WHERE organisation_id = ? AND case_reference IS NOT NULL",
            Integer.class, ORGANISATION_ID);
    }

    private int pendingTasks(String taskName, long reference) {
        return jdbcTemplate.queryForObject(
            "SELECT count(*) FROM scheduled_tasks WHERE task_name = ? AND convert_from(task_data, 'UTF8') LIKE ?",
            Integer.class, taskName, "%" + reference + "%");
    }

    private static String claimElement(String fixture) throws IOException {
        String xml = Files.readString(FIXTURES.resolve(fixture));
        Matcher matcher = CLAIM_ELEMENT.matcher(xml);
        assertThat(matcher.find()).as("RentalClaim element in " + fixture).isTrue();
        return matcher.group();
    }

    private static String claimSet(String... claims) {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <RentalClaimSet xmlns="http://hmcts.gov.uk/pcs/bulkclaim/v2">
              <Version>2.0</Version>
              <PaymentAccountNumber>%s</PaymentAccountNumber>
              %s
            </RentalClaimSet>
            """.formatted(PBA_ACCOUNT, String.join("\n", claims));
    }

    private static Document parse(String xml) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(new InputSource(new StringReader(xml)));
    }

    private static String text(Object context, String path) throws Exception {
        return (String) XPathFactory.newInstance().newXPath()
            .evaluate(path, context, XPathConstants.STRING);
    }

    private static List<Node> nodes(Object context, String path) throws Exception {
        NodeList list = (NodeList) XPathFactory.newInstance().newXPath()
            .evaluate(path, context, XPathConstants.NODESET);
        return IntStream.range(0, list.getLength()).mapToObj(list::item).toList();
    }
}
