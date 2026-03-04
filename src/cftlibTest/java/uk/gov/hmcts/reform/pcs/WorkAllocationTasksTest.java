package uk.gov.hmcts.reform.pcs;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.pcs.ccd.CaseType;
import uk.gov.hmcts.reform.pcs.ccd.domain.CompletionNextStep;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.taskmanagement.model.TaskType;
import uk.gov.hmcts.rse.ccd.lib.test.CftlibTest;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.caseworkerValidateClaim;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.createPossessionClaim;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.resumePossessionClaim;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
@TestPropertySource(properties = {
    "spring.sql.init.mode=always",
    "spring.sql.init.schema-locations=classpath:init-task-outbox.sql"
})
@Disabled
class WorkAllocationTasksTest extends CftlibTest {

    @Autowired
    private CoreCaseDataApi ccdApi;
    @Autowired
    private IdamClient idamClient;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private NamedParameterJdbcTemplate db;

    private String solicitorIdamToken;
    private String caseworkerIdamToken;
    private String s2sToken;

    @BeforeAll
    void setup() {
        solicitorIdamToken = idamClient.getAccessToken("pcs-solicitor1@test.com", "password");
        caseworkerIdamToken = idamClient.getAccessToken("caseworker@pcs.com", "password");
        s2sToken = generateDummyS2SToken("pcs_api");
    }

    @Test
    void createAndCompleteDefendantListCheckTask() {

        CaseDetails caseDetails = createShellCase();
        long caseReference = caseDetails.getId();

        List<ListValue<DefendantDetails>> additionalDefendants = createAdditionalDefendants(25);

        PCSCase caseData = PCSCase.builder()
            .tenancyLicenceDetails(TenancyLicenceDetails.builder()
                                       .typeOfTenancyLicence(TenancyLicenceType.ASSURED_TENANCY)
                                       .build())
            .defendant1(DefendantDetails.builder()
                            .nameKnown(VerticalYesNo.YES)
                            .firstName("Danny")
                            .lastName("Defendant")
                            .build())
            .addAnotherDefendant(VerticalYesNo.YES)
            .additionalDefendants(additionalDefendants)
            .noticeServed(YesOrNo.NO)
            .completionNextStep(CompletionNextStep.SUBMIT_AND_PAY_NOW)
            .build();

        startAndSubmitUpdateEvent(resumePossessionClaim, caseReference, caseData, solicitorIdamToken);

        assertInitiationOutbox(caseReference, List.of(TaskType.CHECK_MULTIPLE_DEFENDANTS), resumePossessionClaim);

        caseData = PCSCase.builder()
            .defendantListValid(VerticalYesNo.YES)
            .build();

        startAndSubmitUpdateEvent(caseworkerValidateClaim, caseReference, caseData, caseworkerIdamToken);

        assertTerminationOutbox(caseReference, "complete",
                                List.of(TaskType.CHECK_MULTIPLE_DEFENDANTS), caseworkerValidateClaim);
    }

    private CaseDetails createShellCase() {
        PCSCase caseData = PCSCase.builder()
            .propertyAddress(AddressUK.builder()
                                 .addressLine1("123 Baker Street")
                                 .postTown("London")
                                 .postCode("W1A 1AA")
                                 .build()
            )
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .build();

        return startAndSubmitCreationEvent(createPossessionClaim, caseData, solicitorIdamToken);
    }

    @SuppressWarnings("SameParameterValue")
    private CaseDetails startAndSubmitCreationEvent(EventId eventId, PCSCase caseData, String idamToken) {
        StartEventResponse startEventResponse = ccdApi.startCase(
            idamToken,
            s2sToken,
            CaseType.getCaseType(),
            eventId.name()
        );

        CaseDataContent content = CaseDataContent.builder()
            .data(caseData)
            .event(Event.builder().id(eventId.name()).build())
            .eventToken(startEventResponse.getToken())
            .build();

        return ccdApi.submitCaseCreation(idamToken, s2sToken, CaseType.getCaseType(), content);
    }

    @SuppressWarnings("SameParameterValue")
    private void startAndSubmitUpdateEvent(EventId eventId, long caseReference, PCSCase caseData, String idamToken) {
        StartEventResponse startEventResponse = ccdApi.startEvent(
            idamToken,
            s2sToken,
            Long.toString(caseReference),
            eventId.name()
        );

        CaseDataContent content = CaseDataContent.builder()
            .data(caseData)
            .event(Event.builder().id(eventId.name()).build())
            .eventToken(startEventResponse.getToken())
            .build();

        ccdApi.createEvent(idamToken, s2sToken, Long.toString(caseReference), content);
    }

    @SuppressWarnings("SameParameterValue")
    private static List<ListValue<DefendantDetails>> createAdditionalDefendants(int count) {
        return IntStream.rangeClosed(1, count)
            .boxed()
            .map(i -> DefendantDetails.builder()
                .nameKnown(VerticalYesNo.YES)
                .firstName("Additional first name " + i)
                .lastName("Additional last name " + i)
                .build())
            .map(defendantDetails -> new ListValue<>(UUID.randomUUID().toString(), defendantDetails))
            .toList();
    }

    @SuppressWarnings("SameParameterValue")
    private void assertInitiationOutbox(long caseReference, List<TaskType> expectedTaskTypes, EventId eventId) {
        List<String> expectedTypes = expectedTaskTypes.stream().map(Enum::name).distinct().toList();

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            List<Map<String, Object>> rows = db.queryForList(
                "SELECT id, status, last_response_code, payload->'task'->>'type' AS task_type "
                    + "FROM ccd.task_outbox WHERE case_id = :caseId AND action = :action::ccd.task_action",
                Map.of("caseId", String.valueOf(caseReference), "action", "initiate")
            );
            assertThat(rows).hasSize(expectedTypes.size());
            assertThat(rows.stream().map(row -> String.valueOf(row.get("task_type"))).toList())
                .as("Initiation outbox mismatch for event %s", eventId)
                .containsExactlyInAnyOrderElementsOf(expectedTypes);
            rows.forEach(this::assertProcessedRow);
        });
    }

    @SuppressWarnings("SameParameterValue")
    private void assertTerminationOutbox(long caseId,
                                         String action,
                                         List<TaskType> expectedTaskTypes,
                                         EventId eventId) {

        List<String> expectedTypes = expectedTaskTypes.stream().map(Enum::name).distinct().toList();

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            Map<String, Object> row = db.queryForMap(
                "SELECT status, last_response_code, "
                    + "payload->'task_types' AS task_types "
                    + "FROM ccd.task_outbox WHERE case_id = :caseId AND action = :action::ccd.task_action ",
                Map.of("caseId", String.valueOf(caseId), "action", action.toLowerCase(Locale.ROOT))
            );
            assertProcessedRow(row);
            List<String> actualTypes = parseJsonArray(row.get("task_types"));
            assertThat(actualTypes)
                .as("Termination outbox mismatch for event %s action %s", eventId, action)
                .containsExactlyInAnyOrderElementsOf(expectedTypes);
        });
    }

    private void assertProcessedRow(Map<String, Object> row) {
        assertThat(row.get("status")).isEqualTo("PROCESSED");
        Object responseCode = row.get("last_response_code");
        assertThat(responseCode).isNotNull();
        assertThat(((Number) responseCode).intValue()).isIn(200, 201, 204);
    }

    private List<String> parseJsonArray(Object jsonValue) {
        if (jsonValue == null) {
            return List.of();
        }

        try {
            return objectMapper.readValue(String.valueOf(jsonValue), new TypeReference<>() {});
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to parse JSON array: " + jsonValue, ex);
        }
    }

}
