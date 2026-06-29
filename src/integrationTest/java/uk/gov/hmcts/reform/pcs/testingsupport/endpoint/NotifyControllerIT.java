package uk.gov.hmcts.reform.pcs.testingsupport.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.PaymentAgreementEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DefendantResponseRepository;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationRequest;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationResponse;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;
import uk.gov.hmcts.reform.pcs.util.IdamHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@DisplayName("NotifyController Integration Tests")
class NotifyControllerIT extends AbstractPostgresContainerIT {

    private static final String SCHEDULED_STATUS = "SCHEDULED";
    private static final String AUTH_HEADER = "Bearer token";
    private static final String SERVICE_AUTH_HEADER = "ServiceAuthToken";
    private static final String SYSTEM_USER_ID_TOKEN = "system-user-id-token";
    private static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @Autowired
    private IdamHelper idamHelper;

    @MockitoBean
    private AuthTokenGenerator authTokenGenerator;
    @MockitoBean
    private OAuth2AuthorizedClientManager authorizedClientManager;
    @MockitoBean
    private NotificationService notificationService;
    @MockitoBean
    private DefendantResponseRepository defendantResponseRepository;

    public NotifyControllerIT(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @BeforeEach
    void setUp() {
        idamHelper.stubIdamSystemUser(authorizedClientManager, SYSTEM_USER_ID_TOKEN);

        EmailNotificationResponse mockResponse = new EmailNotificationResponse();
        mockResponse.setTaskId("task-123");
        mockResponse.setStatus(SCHEDULED_STATUS);
        mockResponse.setNotificationId(UUID.randomUUID());

        when(notificationService.scheduleEmailNotification(
            any(EmailNotificationRequest.class),
            any(PcsCaseEntity.class),
            any(ClaimEntity.class),
            any(PartyEntity.class)
        )).thenReturn(mockResponse);
    }

    @Nested
    @DisplayName("Defendant Response Email Notifications")
    class DefendantResponseNotificationsTest {

        @Test
        @DisplayName("Should send all defendant response emails successfully")
        void shouldSendAllDefendantResponseEmailsSuccessfully() throws Exception {
            PartyEntity party = new PartyEntity();
            party.setEmailAddress("test@example.com");
            party.setFirstName("John");
            party.setLastName("Doe");

            PcsCaseEntity pcsCase = new PcsCaseEntity();
            pcsCase.setId(UUID.randomUUID());
            pcsCase.setCaseReference(1234567890L);

            PaymentAgreementEntity paymentAgreement = new PaymentAgreementEntity();
            paymentAgreement.setId(UUID.randomUUID());

            DefendantResponseEntity defendantResponse = new DefendantResponseEntity();
            defendantResponse.setParty(party);
            defendantResponse.setPcsCase(pcsCase);
            defendantResponse.setPaymentAgreement(paymentAgreement);

            UUID defendantResponseId = UUID.randomUUID();
            when(defendantResponseRepository.findById(defendantResponseId))
                .thenReturn(Optional.of(defendantResponse));

            EmailNotificationResponse response = new EmailNotificationResponse();
            response.setTaskId("task-123");
            response.setStatus(SCHEDULED_STATUS);
            response.setNotificationId(UUID.randomUUID());

            when(notificationService.sendDefendantResponseNoCounterclaimEmailNotification(defendantResponse))
                .thenReturn(response);
            when(notificationService
                     .sendDefendantResponseCounterclaimPaymentRequiredEmailNotification(defendantResponse)
            ).thenReturn(response);
            when(notificationService.sendDefendantResponseCounterclaimPaymentSuccessEmailNotification(any(), any())
            ).thenReturn(response);
            when(notificationService
                     .sendDefendantResponseCounterclaimNoPaymentRequiredEmailNotification(defendantResponse)
            ).thenReturn(response);

            mockMvc.perform(post("/testing-support/send-defendant-response-emails")
                                .param("defendantResponseId", defendantResponseId.toString())
                                .header(AUTHORIZATION, AUTH_HEADER)
                                .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(4)))
                .andExpect(jsonPath("$[0].taskId", is(notNullValue())))
                .andExpect(jsonPath("$[0].status", is(SCHEDULED_STATUS)));
        }

        @Test
        @DisplayName("Should return 404 when defendant response not found")
        void shouldReturn404WhenDefendantResponseNotFound() throws Exception {
            UUID defendantResponseId = UUID.randomUUID();

            when(defendantResponseRepository.findById(defendantResponseId))
                .thenReturn(Optional.empty());

            mockMvc.perform(post("/testing-support/send-defendant-response-emails")
                                .param("defendantResponseId", defendantResponseId.toString())
                                .header(AUTHORIZATION, AUTH_HEADER)
                                .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER))
                .andExpect(status().isNotFound());
        }
    }

    private EmailNotificationRequest createValidEmailRequest() {
        Map<String, Object> personalisation = new HashMap<>();
        personalisation.put("name", "Test User");
        personalisation.put("reference", "TEST-REF-123");

        return EmailNotificationRequest.builder()
            .emailAddress("test.user@example.com")
            .templateId("template-abc123")
            .personalisation(personalisation)
            .reference("external-ref-456")
            .emailReplyToId("reply-to-789")
            .build();
    }
}
