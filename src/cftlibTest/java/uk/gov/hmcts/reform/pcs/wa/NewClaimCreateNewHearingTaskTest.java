package uk.gov.hmcts.reform.pcs.wa;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType;
import uk.gov.hmcts.reform.pcs.model.FeePaymentSummary;
import uk.gov.hmcts.reform.pcs.model.TaskManagementResponse;
import uk.gov.hmcts.reform.pcs.model.WaTask;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;
import uk.gov.hmcts.reform.pcs.service.CaseCreationService;
import uk.gov.hmcts.reform.pcs.service.CaseStateService;
import uk.gov.hmcts.reform.pcs.service.FeePaymentService;
import uk.gov.hmcts.reform.pcs.service.TaskManagementService;
import uk.gov.hmcts.rse.ccd.lib.test.CftlibTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class NewClaimCreateNewHearingTaskTest extends CftlibTest {

    @Autowired
    private IdamClient idamClient;

    @Autowired
    private CaseCreationService caseCreationService;

    @Autowired
    private CaseStateService caseStateService;

    @Autowired
    private FeePaymentService feePaymentService;

    @Autowired
    private TaskManagementService taskManagementService;

    @Autowired
    private OAuth2AuthorizedClientManager authorizedClientManager;

    private String solicitorToken;
    private String hearingCentreTeamLeaderToken;

    @BeforeAll
    void setup() {
        solicitorToken = idamClient.getAccessToken("pcs-solicitor1@test.com", "password");
        hearingCentreTeamLeaderToken = new IdamTokenProvider(
            authorizedClientManager,
            "system-user",
            "pcs-hearing-centre-team-leader-01@localhost",
            "password"
        ).getAuthToken();
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "WA_TESTS_ENABLED", matches = "true")
    void createNewClaimCreateNewHearingTask() throws InterruptedException {

        long caseReference = caseCreationService.createMinimalCase(solicitorToken);

        List<FeePaymentSummary> feePaymentSummaries
            = feePaymentService.waitForFeePaymentRequests(caseReference, PaymentCallbackHandlerType.CLAIM);

        feePaymentService.simulatePayments(caseReference, feePaymentSummaries);

        caseStateService.waitForCaseState(caseReference, State.CASE_ISSUED, solicitorToken);

        ResponseEntity<TaskManagementResponse> responseEntity = taskManagementService.search(
            caseReference,
            List.of(TaskType.NEW_CLAIM_CREATE_NEW_HEARING.getId()),
            hearingCentreTeamLeaderToken
        );

        TaskManagementResponse responseBody = responseEntity.getBody();
        assertThat(responseBody).isNotNull();

        List<WaTask> tasks =  responseBody.getTasks();
        assertThat(tasks).hasSize(1);
        assertThat(tasks.getFirst().getType()).isEqualTo(TaskType.NEW_CLAIM_CREATE_NEW_HEARING.getId());
    }
}
