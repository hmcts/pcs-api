package uk.gov.hmcts.reform.pcs;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.pcs.service.CaseCreationService;
import uk.gov.hmcts.rse.ccd.lib.test.CftlibTest;

import static org.assertj.core.api.Assertions.assertThat;

import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.pcs.client.CcdClient;
import uk.gov.hmcts.reform.pcs.service.CaseCreationService;
import uk.gov.hmcts.rse.ccd.lib.test.CftlibTest;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
class CaseCreationServiceTest extends CftlibTest {
    @Autowired
    private CaseCreationService caseCreationService;

    @Autowired
    private IdamClient idamClient;

    @Test
    void runCreateMaximalCase() {
        String testAuthorisationToken = idamClient.getAccessToken("pcs-solicitor1@test.com", "password");

        long caseReference = caseCreationService.createMaximalCase(testAuthorisationToken);

        System.out.println("==========================================");
        System.out.println("CREATED MAXIMAL CASE ID: " + caseReference);
        System.out.println("==========================================");

        Assertions.assertThat(caseReference).isGreaterThan(0L);
    }
}
