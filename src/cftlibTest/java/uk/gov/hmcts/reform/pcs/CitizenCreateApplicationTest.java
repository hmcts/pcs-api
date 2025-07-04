package uk.gov.hmcts.reform.pcs;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.rse.ccd.lib.test.CftlibTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
class CitizenCreateApplicationTest extends CftlibTest {

    @Autowired
    private CoreCaseDataApi ccdApi;

    @Autowired
    private IdamClient idamClient;

    private String idamToken;
    private String s2sToken;
    private Long caseReference;

    @BeforeAll
    void setup() {
        idamToken = idamClient.getAccessToken("citizen@pcs.com", "password");
        s2sToken = generateDummyS2SToken("ccd_gw");
    }

    @Test
    @Order(1)
    void citizenCreatesApplication() {

    }




}
