package uk.gov.hmcts.reform.pcs;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.rse.ccd.lib.test.CftlibTest;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestWithCCD extends CftlibTest {

    @Autowired
    private CoreCaseDataApi ccdApi;

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
    public void createsTestCaseDecentralised() throws JsonProcessingException {
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
}
