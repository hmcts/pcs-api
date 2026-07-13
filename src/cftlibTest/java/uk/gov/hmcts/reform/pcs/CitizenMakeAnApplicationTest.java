package uk.gov.hmcts.reform.pcs;


import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.CitizenGenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.client.CcdClient;
import uk.gov.hmcts.reform.pcs.client.TestingSupportClient;
import uk.gov.hmcts.reform.pcs.model.PartyAccessCode;
import uk.gov.hmcts.reform.pcs.notification.CapturedNotification;
import uk.gov.hmcts.reform.pcs.notification.MockNotificationServer;
import uk.gov.hmcts.reform.pcs.service.AccessCodeService;
import uk.gov.hmcts.reform.pcs.service.CaseCreationService;
import uk.gov.hmcts.reform.pcs.testingsupport.model.PartyEmail;
import uk.gov.hmcts.rse.ccd.lib.test.CftlibTest;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CitizenMakeAnApplicationTest extends CftlibTest {

    private static final String CITIZEN_EMAIL_ADDRESS = "test@test.com";
    private static final String GEN_APP_NOTIFICATION_TEMPLATE_ID = "e5daba0d-5c66-4c76-9e0f-06646200dc89";

    @Autowired
    private CcdClient ccdClient;
    @Autowired
    private IdamClient idamClient;
    @Autowired
    private CaseCreationService caseCreationService;
    @Autowired
    private AccessCodeService accessCodeService;
    @Autowired
    private TestingSupportClient testingSupportClient;

    private final MockNotificationServer mockNotificationServer = new MockNotificationServer();

    private String solicitorToken;
    private String citizenToken;

    @BeforeAll
    void setup() {
        solicitorToken = idamClient.getAccessToken("pcs-solicitor1@test.com", "password");
        citizenToken = idamClient.getAccessToken("citizen@pcs.com", "password");

        mockNotificationServer.startServer();
    }

    @AfterAll
    void tearDown() {
        mockNotificationServer.stopServer();
    }

    @BeforeEach
    void setUp() {
        mockNotificationServer.resetServer();
    }

    @Test
    void makeAnApplication() {
        long caseReference = caseCreationService.createMinimalCase(solicitorToken);

        Awaitility.await("Case issued")
            .atMost(Duration.ofSeconds(120))
            .pollInterval(Duration.ofMillis(1000))
            .ignoreExceptions()
            .until(() -> caseIssued(caseReference));


        List<PartyAccessCode> partyAccessCodes = accessCodeService.waitForAccessCodes(caseReference);
        PartyAccessCode partyAccessCode = partyAccessCodes.getFirst();
        accessCodeService.linkUserToCase(caseReference, partyAccessCode.getAccessCode(), citizenToken);

        PartyEmail partyEmail = PartyEmail.builder()
            .partyId(partyAccessCode.getPartyId())
            .emailAddress(CITIZEN_EMAIL_ADDRESS)
            .build();
        testingSupportClient.setPartyEmail(partyEmail, citizenToken);

        CitizenGenAppRequest citizenGenAppRequest = CitizenGenAppRequest.builder()
            .applicationType(GenAppType.ADJOURN)
            .needHwf(VerticalYesNo.NO)
            .withoutNotice(VerticalYesNo.NO)
            .hasSupportingDocuments(VerticalYesNo.NO)
            .sotAccepted(VerticalYesNo.YES)
            .sotFullName("Test party name")
            .build();

        PCSCase caseData = PCSCase.builder()
            .citizenGenAppRequest(citizenGenAppRequest)
            .build();

        ccdClient.updateCase(EventId.makeAnApplication, caseReference, caseData, citizenToken);

        List<CapturedNotification> sentNotifications = mockNotificationServer.getSentNotifications(1);

        CapturedNotification sentNotification = sentNotifications.getFirst();
        assertThat(sentNotification.getTemplateId()).isEqualTo(GEN_APP_NOTIFICATION_TEMPLATE_ID);
        assertThat(sentNotification.getEmailAddress()).isEqualTo(CITIZEN_EMAIL_ADDRESS);
        assertThat(sentNotification.getPersonalisation())
            .containsKeys("caseNumber", "claimantName", "firstName", "lastName", "primaryDefendantName");
        assertThat(sentNotification.getReference()).isNotBlank();
    }

    private boolean caseIssued(long caseReference) {
        CaseDetails caseDetails = ccdClient.getCaseDetails(caseReference, solicitorToken);
        return Objects.equals(caseDetails.getState(), State.CASE_ISSUED.name());

    }

}
