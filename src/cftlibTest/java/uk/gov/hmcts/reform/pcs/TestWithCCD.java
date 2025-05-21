package uk.gov.hmcts.reform.pcs;


import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.ccd.client.CaseEventsApi;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.rse.ccd.lib.test.CftlibTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestWithCCD extends CftlibTest {

    @Autowired
    private CoreCaseDataApi ccdApi;

    @Autowired
    CaseEventsApi ccdEventsApi;

    @Autowired
    private IdamClient idamClient;

    @Autowired
    private ObjectMapper mapper;

    CaseDetails caseDetails;
    String idamToken;
    String s2sToken;
    String userId;

    @BeforeAll
    public void setup() {
        idamToken = idamClient.getAccessToken("caseworker@pcs.com", "password");
        s2sToken = generateDummyS2SToken("ccd_gw");
        userId = idamClient.getUserInfo(idamToken).getUid();
    }

    @Order(1)
    @Test
    public void createsTestCase() {
        testCreateCase("createTestApplication");
    }

    @Order(2)
    @Test
    public void createsTestCaseDecentralised() {
        testCreateCase("createTestApplicationDecentralised");
    }

    @SneakyThrows
    private void testCreateCase(String event) {
        var r = ccdApi.startCase(idamToken, s2sToken, "PCS", event);
        var content = CaseDataContent.builder()
            .data(PCSCase.builder().caseDescription("Foo").build())
            .event(Event.builder().id(event).build())
            .eventToken(r.getToken())
            .build();
        caseDetails = ccdApi.submitForCaseworker(idamToken, s2sToken, userId,
            "CIVIL", "PCS", false, content);
        assertThat(caseDetails.getId()).isNotNull();
        var result = mapper.readValue(mapper.writeValueAsString(caseDetails.getData()), PCSCase.class);
        assertThat(result.getCaseDescription()).isEqualTo("Foo");
    }

    @Order(3)
    @Test
    public void getEventHistory() throws Exception {
        var events = ccdEventsApi.findEventDetailsForCase(idamToken, s2sToken, userId, "CIVIL", "PCS",
            caseDetails.getId().toString());
        assertThat(events.size()).isEqualTo(1);

        var firstEvent = events.getFirst();
        MatcherAssert.assertThat(firstEvent.getStateId(), equalTo("Open"));
        MatcherAssert.assertThat(firstEvent.getStateName(), equalTo("Open"));
    }

    @Order(6)
    @SneakyThrows
    @Test
    void searchCases() {
        // Give some time to index the case created by the previous test
        Thread.sleep(600);
        await()
            .timeout(Duration.ofSeconds(10))
            .ignoreExceptions()
            .until(this::caseAppearsInSearch);
    }

    @SneakyThrows
    private Boolean caseAppearsInSearch() {
        var query = String.format("""
            {
              "native_es_query":{"from":0,"query":{"bool":{"must":[
              {"term":{"reference":"%d"}}
              ]}},"size":25,"sort":[{"_id": "desc"}]},
              "supplementary_data":["*"]
            }""", caseDetails.getId());
        var result = ccdApi.searchCases(idamToken, s2sToken, "PCS", query);
        assertThat(result.getTotal()).isEqualTo(1);

        assertThat(result.getCases().getFirst().getData().get("caseDescription")).isEqualTo("Foo");

        return true;
    }
}
