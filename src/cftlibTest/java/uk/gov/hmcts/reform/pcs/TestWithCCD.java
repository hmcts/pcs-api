package uk.gov.hmcts.reform.pcs;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.rse.ccd.lib.test.CftlibTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestWithCCD extends CftlibTest {

    @Autowired
    private CoreCaseDataApi ccdApi;

    @Autowired
    private IdamClient idamClient;

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
}
