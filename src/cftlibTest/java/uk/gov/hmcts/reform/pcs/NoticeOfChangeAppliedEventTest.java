package uk.gov.hmcts.reform.pcs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kagkarlsson.scheduler.task.Execution;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.client.RestClient;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.OrganisationProfile;
import uk.gov.hmcts.reform.pcs.ccd.model.NocAccessChangeTaskData;
import uk.gov.hmcts.reform.pcs.ccd.task.NocAccessChangeTaskComponent;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;
import uk.gov.hmcts.reform.pcs.service.CaseCreationService;
import uk.gov.hmcts.rse.ccd.lib.test.CftlibTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NoticeOfChangeAppliedEventTest extends CftlibTest {

    private static final String EVENT_ID = "noticeOfChangeApplied";
    private static final String LEGAL_REP_EMAIL = "james-solicitor-user1@test.com";
    private static final String ACTING_SOLICITOR_ID = "40460563-4f42-479f-995f-4dc77399ade1";
    private static final String ORGANISATION_ID = "TEST-123";
    private static final String EXPECTED_ACCESS_GROUP =
        "PCS:PCS:solicitor-org-defendant-access:defendant-solicitor:" + ORGANISATION_ID;
    private static final String SYSTEM_USER_ID = "78acf0a0-079b-3112-8cad-549c81b83510";
    private static final Instant EXECUTION_TIME = Instant.parse("2026-09-04T09:30:00Z");

    @Autowired
    private CaseCreationService caseCreationService;

    @Autowired
    private NocAccessChangeTaskComponent nocAccessChangeTaskComponent;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private IdamClient idamClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void appliesNoticeOfChangeAndRecordsOneAttributedSystemEventFromABackgroundThread() throws Exception {
        long caseReference = caseCreationService.createMinimalCase(
            idamClient.getAccessToken("pcs-solicitor1@test.com", "password")
        );
        Map<String, Object> caseParameters = Map.of("caseReference", caseReference);
        UUID defendantId = jdbcTemplate.queryForObject(
            """
                select cp.party_id
                  from pcs_case pc
                  join claim c on c.case_id = pc.id
                  join claim_party cp on cp.claim_id = c.id
                 where pc.case_reference = :caseReference
                   and cp.role = 'DEFENDANT'
                """,
            caseParameters,
            UUID.class
        );

        NocAccessChangeTaskData taskData = NocAccessChangeTaskData.builder()
            .partyId(defendantId.toString())
            .organisationDetailsResponse(testOrganisation())
            .userId(ACTING_SOLICITOR_ID)
            .email(LEGAL_REP_EMAIL)
            .firstName("James")
            .lastName("Solicitor")
            .caseReference(Long.toString(caseReference))
            .eventIdempotencyKey(UUID.randomUUID())
            .build();
        TaskInstance<NocAccessChangeTaskData> taskInstance = new TaskInstance<>(
            "noc-access-change-task",
            "noc-" + caseReference,
            taskData
        );
        ExecutionContext executionContext = new ExecutionContext(
            null,
            new Execution(EXECUTION_TIME, taskInstance),
            null,
            null
        );

        runTaskInBackground(taskInstance, executionContext);

        Map<String, Object> event = jdbcTemplate.queryForMap(
            """
                select ce.id,
                       ce.event_id,
                       ce.event_name,
                       ce.summary,
                       ce.user_id,
                       ce.proxied_by,
                       ce.data::text as data
                  from ccd.case_event ce
                  join ccd.case_data cd on cd.id = ce.case_data_id
                 where cd.reference = :caseReference
                   and ce.event_id = :eventId
                """,
            Map.of("caseReference", caseReference, "eventId", EVENT_ID)
        );
        assertThat(event)
            .containsEntry("event_id", EVENT_ID)
            .containsEntry("event_name", "Notice of change applied")
            .containsEntry("summary", "Notice of change by " + LEGAL_REP_EMAIL)
            .containsEntry("user_id", SYSTEM_USER_ID)
            .containsEntry("proxied_by", ACTING_SOLICITOR_ID);

        JsonNode snapshot = objectMapper.readTree((String) event.get("data"));
        assertThat(hasExpectedAccessGroup(snapshot)).isTrue();
        assertThat(activeOrganisationLinks(defendantId)).isEqualTo(1);
        assertThat(auditedOrganisationLinks(event.get("id"))).isEqualTo(1);

        long caseDataId = jdbcTemplate.queryForObject(
            "select id from ccd.case_data where reference = :caseReference",
            caseParameters,
            Long.class
        );
        await()
            .pollInterval(Duration.ofSeconds(1))
            .atMost(Duration.ofSeconds(75))
            .untilAsserted(() -> assertThat(hasExpectedAccessGroup(indexedCase(caseDataId))).isTrue());

        runTaskInBackground(taskInstance, executionContext);

        assertThat(jdbcTemplate.queryForObject(
            """
                select count(*)
                  from ccd.case_event ce
                  join ccd.case_data cd on cd.id = ce.case_data_id
                 where cd.reference = :caseReference
                   and ce.event_id = :eventId
                """,
            Map.of("caseReference", caseReference, "eventId", EVENT_ID),
            Integer.class
        )).isEqualTo(1);
        assertThat(activeOrganisationLinks(defendantId)).isEqualTo(1);
    }

    private void runTaskInBackground(
        TaskInstance<NocAccessChangeTaskData> taskInstance,
        ExecutionContext executionContext
    ) throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            executor.submit(() -> nocAccessChangeTaskComponent.nocAccessChangeTask()
                .execute(taskInstance, executionContext)).get();
        } finally {
            executor.shutdown();
        }
    }

    private OrganisationDetailsResponse testOrganisation() {
        return OrganisationDetailsResponse.builder()
            .name("Test Organisation")
            .organisationIdentifier(ORGANISATION_ID)
            .organisationProfileIds(List.of(OrganisationProfile.SOLICITOR_PROFILE.getId()))
            .contactInformation(List.of(OrganisationDetailsResponse.ContactInformation.builder()
                .addressLine1("1 Test Street")
                .townCity("London")
                .postCode("SW1A 1AA")
                .build()))
            .build();
    }

    private int activeOrganisationLinks(UUID defendantId) {
        return jdbcTemplate.queryForObject(
            """
                select count(*)
                  from claim_party_organisation cpo
                  join organisation o on o.id = cpo.organisation_id
                 where cpo.party_id = :partyId
                   and cpo.active = 'YES'
                   and o.organisation_id = :organisationId
                """,
            Map.of("partyId", defendantId, "organisationId", ORGANISATION_ID),
            Integer.class
        );
    }

    private int auditedOrganisationLinks(Object eventId) {
        return jdbcTemplate.queryForObject(
            """
                select count(*)
                  from ccd.audit_log audit
                 where audit.case_event_id = :eventId
                   and audit.operation = 'INSERT'
                   and audit.table_schema = 'public'
                   and audit.table_name = 'claim_party_organisation'
                """,
            Map.of("eventId", eventId),
            Integer.class
        );
    }

    private JsonNode indexedCase(long caseDataId) throws Exception {
        String response = RestClient.create("http://localhost:9200")
            .get()
            .uri("/pcs_cases/_doc/{caseDataId}", caseDataId)
            .retrieve()
            .body(String.class);
        return objectMapper.readTree(response).path("_source");
    }

    private boolean hasExpectedAccessGroup(JsonNode caseData) {
        JsonNode data = caseData.has("data") ? caseData.path("data") : caseData;
        for (JsonNode group : data.path("CaseAccessGroups")) {
            if (EXPECTED_ACCESS_GROUP.equals(group.path("value").path("caseAccessGroupId").asText())) {
                return true;
            }
        }
        return false;
    }
}
