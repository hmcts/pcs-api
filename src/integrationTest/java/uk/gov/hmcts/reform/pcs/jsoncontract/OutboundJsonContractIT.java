package uk.gov.hmcts.reform.pcs.jsoncontract;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import feign.codec.Decoder;
import feign.codec.Encoder;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.FeignClientFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRolesRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRolesResource;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRolesResponse;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.ccd.document.am.model.CaseDocumentsMetadata;
import uk.gov.hmcts.reform.ccd.document.am.model.Document;
import uk.gov.hmcts.reform.docassembly.domain.DocAssemblyRequest;
import uk.gov.hmcts.reform.docassembly.domain.DocAssemblyResponse;
import uk.gov.hmcts.reform.fees.client.model.FeeLookupResponseDto;
import uk.gov.hmcts.reform.payments.request.CardPaymentServiceRequestDTO;
import uk.gov.hmcts.reform.payments.request.CreateServiceRequestDTO;
import uk.gov.hmcts.reform.payments.request.PBAServiceRequestDTO;
import uk.gov.hmcts.reform.payments.response.CardPaymentServiceRequestResponse;
import uk.gov.hmcts.reform.payments.response.PBAServiceRequestResponse;
import uk.gov.hmcts.reform.payments.response.PaymentServiceResponse;
import uk.gov.hmcts.reform.pcs.am.RoleAssignmentResponse;
import uk.gov.hmcts.reform.pcs.camunda.SendMessageRequest;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.hearings.model.DeleteHearingRequest;
import uk.gov.hmcts.reform.pcs.hearings.model.GetHearingsResponse;
import uk.gov.hmcts.reform.pcs.hearings.model.HearingRequest;
import uk.gov.hmcts.reform.pcs.hearings.model.HearingResponse;
import uk.gov.hmcts.reform.pcs.hearings.model.UpdateHearingRequest;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;
import uk.gov.hmcts.reform.sendletter.api.SendLetterResponse;
import uk.gov.hmcts.reform.sendletter.api.model.v3.LetterV3;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static uk.gov.hmcts.reform.pcs.jsoncontract.JsonContract.assertSameTree;
import static uk.gov.hmcts.reform.pcs.jsoncontract.JsonContract.parse;
import static uk.gov.hmcts.reform.pcs.jsoncontract.JsonContract.readBaseline;
import static uk.gov.hmcts.reform.pcs.jsoncontract.JsonContract.writeBaseline;

/**
 * Pins the JSON pcs-api exchanges with other services, using each Feign client's own encoder and decoder
 * from the running application (so per-client configuration, converters and mappers are all exercised).
 *
 * <ul>
 *   <li>Requests pcs-api sends: a fully populated request (fixed Instancio seed) must encode to exactly
 *   the committed baseline.</li>
 *   <li>Responses pcs-api reads: the committed baseline must decode and re-encode without losing or
 *   changing any value.</li>
 * </ul>
 *
 * <p>The baselines under {@code src/integrationTest/resources/json-contract/outbound/} were generated on
 * Spring Boot 3. A failure after a framework or client-library upgrade means the upgrade changed what
 * pcs-api sends or how it reads a downstream response: fix the upgrade, do not regenerate the baseline.
 * Regenerate only for an intentional model change, with {@code JSON_CONTRACT_UPDATE=true}.
 */
@SpringBootTest
@ActiveProfiles("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Outbound JSON contract")
class OutboundJsonContractIT extends AbstractPostgresContainerIT {

    private static final long SEED = 9529L;
    private static final String CASE_DATA_BASELINE = "pcs-case.primary-mapper.json";

    private static final List<Contract> CONTRACTS = List.of(
        // CCD data store and case assignment (core-case-data-store-client)
        request("core-case-data-api", CaseDataContent.class),
        response("core-case-data-api", StartEventResponse.class),
        response("core-case-data-api", CaseDetails.class),
        request("case-assignment-api", CaseAssignmentUserRolesRequest.class),
        response("case-assignment-api", CaseAssignmentUserRolesResource.class),
        response("case-assignment-api", CaseAssignmentUserRolesResponse.class),
        // CCD case document AM (ccd-case-document-am-client)
        request("case-document-am-client-api", CaseDocumentsMetadata.class),
        response("case-document-am-client-api", Document.class),
        // Docmosis via doc assembly
        request("doc-assembly-api", DocAssemblyRequest.class),
        response("doc-assembly-api", DocAssemblyResponse.class),
        // Fees and payments
        response("fees-api", FeeLookupResponseDto.class),
        request("payments-api", CreateServiceRequestDTO.class),
        request("payments-api", CardPaymentServiceRequestDTO.class),
        request("payments-api", PBAServiceRequestDTO.class),
        response("payments-api", PaymentServiceResponse.class),
        response("payments-api", CardPaymentServiceRequestResponse.class),
        response("payments-api", PBAServiceRequestResponse.class),
        // Bulk print
        request("send-letter-api", LetterV3.class),
        response("send-letter-api", SendLetterResponse.class),
        // Hearings (HMC)
        request("hmc-hearing", HearingRequest.class),
        request("hmc-hearing", UpdateHearingRequest.class),
        request("hmc-hearing", DeleteHearingRequest.class),
        response("hmc-hearing", HearingResponse.class),
        response("hmc-hearing", GetHearingsResponse.class),
        // Work allocation
        request("wa-workflow-api", SendMessageRequest.class),
        // Access management, reference data and IDAM
        response("role-assignment", RoleAssignmentResponse.class),
        response("location-reference", new ParameterizedTypeReference<List<CourtVenue>>() { }.getType(),
                 "CourtVenueList"),
        response("rd-professional", OrganisationDetailsResponse.class),
        response("idam-userinfo", UserInfo.class)
    );

    @Autowired
    private FeignClientFactory feignClients;

    @Autowired
    private ObjectMapper primaryMapper;

    /** The fully populated case from the PCSCase contract, used as the CCD case data payload. */
    private JsonNode caseData;

    @BeforeAll
    void setUp() throws IOException {
        caseData = readBaseline(CASE_DATA_BASELINE);
        if (JsonContract.UPDATE) {
            for (Contract contract : CONTRACTS) {
                try {
                    writeBaseline(contract.baseline(), encode(contract, sample(contract)));
                } catch (RuntimeException e) {
                    throw new IllegalStateException("Could not generate baseline for " + contract.displayName(), e);
                }
            }
        }
    }

    @TestFactory
    Stream<DynamicTest> outboundContracts() {
        return CONTRACTS.stream().map(contract -> DynamicTest.dynamicTest(contract.displayName(), () -> {
            JsonNode baseline = readBaseline(contract.baseline());
            if (contract.request()) {
                assertSameTree(contract.displayName(), baseline, encode(contract, sample(contract)));
            } else {
                Object decoded = decode(contract, baseline);
                assertSameTree(contract.displayName() + " (decode -> encode)", baseline, encode(contract, decoded));
            }
        }));
    }

    private JsonNode encode(Contract contract, Object value) {
        Encoder encoder = feignClients.getInstance(contract.client(), Encoder.class);
        RequestTemplate template = new RequestTemplate();
        template.header("Content-Type", "application/json");
        encoder.encode(value, contract.type(), template);
        return parse(new String(template.body(), StandardCharsets.UTF_8));
    }

    private Object decode(Contract contract, JsonNode json) throws IOException {
        Decoder decoder = feignClients.getInstance(contract.client(), Decoder.class);
        Request request = Request.create(Request.HttpMethod.GET, "http://localhost/json-contract",
                                         Map.of(), null, StandardCharsets.UTF_8, null);
        Response response = Response.builder()
            .status(200)
            .reason("OK")
            .request(request)
            .headers(Map.<String, Collection<String>>of("Content-Type", List.of("application/json")))
            .body(json.toString(), StandardCharsets.UTF_8)
            .build();
        return decoder.decode(response, contract.type());
    }

    private static Contract request(String client, Class<?> type) {
        return new Contract(client, type, type.getSimpleName(), true);
    }

    private static Contract response(String client, Class<?> type) {
        return new Contract(client, type, type.getSimpleName(), false);
    }

    private static Contract response(String client, Type type, String name) {
        return new Contract(client, type, name, false);
    }

    private record Contract(String client, Type type, String name, boolean request) {

        String baseline() {
            return "outbound/" + client + "/" + name + ".json";
        }

        String displayName() {
            return client + " " + (request ? "request " : "response ") + name;
        }
    }

    /**
     * A fully populated instance of the contract type, with a fixed seed.
     *
     * <p>Free-form fields declared as plain {@code Object} would otherwise get a bare {@code new Object()},
     * which no real payload carries and Jackson cannot write, so they are left unset. The exceptions are the
     * CCD case data payloads, which carry the full PCSCase JSON as in production (field selectors take
     * precedence over the type selector). Lenient, because most types have none of these fields.
     */
    private Object sample(Contract contract) {
        Map<String, Object> caseDataMap = primaryMapper.convertValue(caseData, new TypeReference<>() { });
        if (contract.type() instanceof Class<?> cls) {
            return Instancio.of(cls)
                .set(Select.all(Object.class), null)
                .set(Select.field(CaseDataContent.class, "data"), caseData)
                .set(Select.field(CaseDetails.class, "data"), caseDataMap)
                .lenient()
                .withSeed(SEED).withSettings(JsonContract.instancioSettings()).create();
        }
        // List<CourtVenue> is the only generic contract
        return Instancio.ofList(CourtVenue.class).size(2)
            .set(Select.all(Object.class), null)
            .lenient()
            .withSeed(SEED).withSettings(JsonContract.instancioSettings()).create();
    }
}
