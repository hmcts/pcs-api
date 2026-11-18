package uk.gov.hmcts.reform.pcs;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.pcs.ccd.domain.AcceptReject;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.EnforcementDecision;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RejectionReason;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.client.CcdClient;
import uk.gov.hmcts.reform.pcs.notification.CapturedNotification;
import uk.gov.hmcts.reform.pcs.notification.MockNotificationServer;
import uk.gov.hmcts.reform.pcs.service.CaseCreationService;
import uk.gov.hmcts.reform.pcs.service.EnforcementApplicationCreationService;
import uk.gov.hmcts.rse.ccd.lib.test.CftlibTest;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
class EnforcementEmailsTest extends CftlibTest {

    private static final String WARRANT_SUSPENDED_TEMPLATE_ID = "dd";

    @Autowired
    private IdamClient idamClient;
    @Autowired
    private CcdClient ccdClient;
    @Autowired
    private CaseCreationService caseCreationService;
    @Autowired
    private EnforcementApplicationCreationService enforcementApplicationCreationService;
    @Autowired
    private MockNotificationServer mockNotificationServer;
    @Autowired
    private ObjectMapper objectMapper;

    private String solicitorToken;
    private String caseworkerToken;

    private Long caseReference;

    @BeforeAll
    void setup() {
        solicitorToken = idamClient.getAccessToken("pcs-solicitor1@test.com", "password");
        caseworkerToken = idamClient.getAccessToken("pcs-hearing-centre-team-leader-01@localhost", "password");
    }

    @Test
    @Order(1)
    void verifyWarrantApplicationRejectionEmail() {
        Instant testStart = Instant.now();

        caseReference = caseCreationService.createAndIssueCase(solicitorToken, LanguageUsed.ENGLISH);

        enforcementApplicationCreationService.createMinimalWarrantApplication(caseReference, solicitorToken);


        StartEventResponse startEventResponse = ccdClient.startEvent(
            EventId.decideEnforcement,
            caseReference,
            caseworkerToken
        );

        Map<String, Object> data = startEventResponse.getCaseDetails().getData();
        PCSCase startEventData = objectMapper.convertValue(data, PCSCase.class);

        EnforcementDecision enforcementDecision = EnforcementDecision.builder()
            .warrantId(startEventData.getEnforcementDecision().getWarrantId())
            .acceptReject(AcceptReject.REJECT)
            .rejectionReason(RejectionReason.ORDER_SET_ASIDE_OR_SUSPENDED)
            .setAsideOrSuspendedDetails("Details about why the order was set aside")
            .build();

        PCSCase eventData = PCSCase.builder()
            .enforcementDecision(enforcementDecision)
            .build();

        ccdClient.submitEvent(
            EventId.decideEnforcement,
            caseReference,
            eventData,
            startEventResponse.getToken(),
            caseworkerToken
        );

        List<CapturedNotification> sentNotifications
            = mockNotificationServer.getSentNotifications(caseReference, 1, testStart);

        CapturedNotification sentNotification = sentNotifications.getFirst();
        assertThat(sentNotification.getTemplateId()).isEqualTo(WARRANT_SUSPENDED_TEMPLATE_ID);
        assertThat(sentNotification.getEmailAddress()).isEqualTo("foo");
        assertThat(sentNotification.getPersonalisation())
            .containsKeys("caseNumber", "claimantName", "firstName", "lastName", "primaryDefendantName");
        assertThat(sentNotification.getReference()).isNotBlank();

    }

}
