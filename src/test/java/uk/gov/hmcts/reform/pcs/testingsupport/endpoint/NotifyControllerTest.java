package uk.gov.hmcts.reform.pcs.testingsupport.endpoint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.PaymentAgreementEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DefendantResponseRepository;
import uk.gov.hmcts.reform.pcs.notify.exception.NotificationException;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationRequest;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationResponse;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotifyController Tests")
class NotifyControllerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private DefendantResponseRepository defendantResponseRepository;

    private NotifyController notifyController;

    private static final String AUTH_HEADER = "Bearer test-token";
    private static final String SERVICE_AUTH_HEADER = "service-auth-token";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEMPLATE_ID = "template-123";
    private static final String TASK_ID = "task-456";
    private static final String SCHEDULED_STATUS = "SCHEDULED";

    @BeforeEach
    void setUp() {
        notifyController = new NotifyController(notificationService, defendantResponseRepository);
    }

    @Nested
    @DisplayName("Send Email Tests")
    class SendEmailTests {

        @Test
        @DisplayName("Should successfully schedule email notification")
        void shouldSuccessfullyScheduleEmailNotification() {
            EmailNotificationRequest request = createValidEmailRequest();
            EmailNotificationResponse expectedResponse = createEmailResponse();

            when(notificationService.scheduleEmailNotification(any(EmailNotificationRequest.class), any(UUID.class)))
                .thenReturn(expectedResponse);

            ResponseEntity<EmailNotificationResponse> response = notifyController.sendEmail(
                AUTH_HEADER, SERVICE_AUTH_HEADER, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getTaskId()).isEqualTo(TASK_ID);
            assertThat(response.getBody().getStatus()).isEqualTo(SCHEDULED_STATUS);

            verify(notificationService).scheduleEmailNotification(eq(request), any(UUID.class));
        }

        @Test
        @DisplayName("Should handle minimal email request")
        void shouldHandleMinimalEmailRequest() {
            EmailNotificationRequest request = EmailNotificationRequest.builder()
                .emailAddress(TEST_EMAIL)
                .templateId(TEMPLATE_ID)
                .build();

            EmailNotificationResponse expectedResponse = createEmailResponse();

            when(notificationService.scheduleEmailNotification(any(EmailNotificationRequest.class), any(UUID.class)))
                .thenReturn(expectedResponse);

            ResponseEntity<EmailNotificationResponse> response = notifyController.sendEmail(
                AUTH_HEADER, SERVICE_AUTH_HEADER, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
            assertThat(response.getBody()).isNotNull();
            verify(notificationService).scheduleEmailNotification(eq(request), any(UUID.class));
        }

        @Test
        @DisplayName("Should handle email request with personalisation")
        void shouldHandleEmailRequestWithPersonalisation() {
            Map<String, Object> personalisation = new HashMap<>();
            personalisation.put("firstName", "John");
            personalisation.put("lastName", "Doe");
            personalisation.put("caseReference", "CASE-123");

            EmailNotificationRequest request = EmailNotificationRequest.builder()
                .emailAddress(TEST_EMAIL)
                .templateId(TEMPLATE_ID)
                .personalisation(personalisation)
                .reference("REF-789")
                .emailReplyToId("reply-to-123")
                .build();

            EmailNotificationResponse expectedResponse = createEmailResponse();

            when(notificationService.scheduleEmailNotification(any(EmailNotificationRequest.class), any(UUID.class)))
                .thenReturn(expectedResponse);

            ResponseEntity<EmailNotificationResponse> response = notifyController.sendEmail(
                AUTH_HEADER, SERVICE_AUTH_HEADER, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getTaskId()).isEqualTo(TASK_ID);

            verify(notificationService).scheduleEmailNotification(eq(request), any(UUID.class));
        }

        @Test
        @DisplayName("Should use default authorization header when not provided")
        void shouldUseDefaultAuthorizationHeaderWhenNotProvided() {
            EmailNotificationRequest request = createValidEmailRequest();
            EmailNotificationResponse expectedResponse = createEmailResponse();

            when(notificationService.scheduleEmailNotification(any(EmailNotificationRequest.class), any(UUID.class)))
                .thenReturn(expectedResponse);

            ResponseEntity<EmailNotificationResponse> response = notifyController.sendEmail(
                "DummyId", SERVICE_AUTH_HEADER, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
            assertThat(response.getBody()).isNotNull();
            verify(notificationService).scheduleEmailNotification(eq(request), any(UUID.class));
        }

        @Test
        @DisplayName("Should return internal server error when service throws exception")
        void shouldReturnInternalServerErrorWhenServiceThrowsException() {
            EmailNotificationRequest request = createValidEmailRequest();

            when(notificationService.scheduleEmailNotification(any(EmailNotificationRequest.class), any(UUID.class)))
                .thenThrow(new NotificationException("Database error", new RuntimeException()));

            ResponseEntity<EmailNotificationResponse> response = notifyController.sendEmail(
                AUTH_HEADER, SERVICE_AUTH_HEADER, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNull();

            verify(notificationService).scheduleEmailNotification(eq(request), any(UUID.class));
        }

        @Test
        @DisplayName("Should return internal server error when service throws runtime exception")
        void shouldReturnInternalServerErrorWhenServiceThrowsRuntimeException() {
            EmailNotificationRequest request = createValidEmailRequest();

            when(notificationService.scheduleEmailNotification(any(EmailNotificationRequest.class), any(UUID.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

            ResponseEntity<EmailNotificationResponse> response = notifyController.sendEmail(
                AUTH_HEADER, SERVICE_AUTH_HEADER, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNull();

            verify(notificationService).scheduleEmailNotification(eq(request), any(UUID.class));
        }

        @Test
        @DisplayName("Should handle empty personalisation map")
        void shouldHandleEmptyPersonalisationMap() {
            EmailNotificationRequest request = EmailNotificationRequest.builder()
                .emailAddress(TEST_EMAIL)
                .templateId(TEMPLATE_ID)
                .personalisation(new HashMap<>())
                .build();

            EmailNotificationResponse expectedResponse = createEmailResponse();

            when(notificationService.scheduleEmailNotification(any(EmailNotificationRequest.class), any(UUID.class)))
                .thenReturn(expectedResponse);

            ResponseEntity<EmailNotificationResponse> response = notifyController.sendEmail(
                AUTH_HEADER, SERVICE_AUTH_HEADER, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
            assertThat(response.getBody()).isNotNull();
            verify(notificationService).scheduleEmailNotification(eq(request), any(UUID.class));
        }

        @Test
        @DisplayName("Should handle personalisation with different data types")
        void shouldHandlePersonalisationWithDifferentDataTypes() {
            Map<String, Object> personalisation = new HashMap<>();
            personalisation.put("stringValue", "test");
            personalisation.put("intValue", 123);
            personalisation.put("booleanValue", true);
            personalisation.put("doubleValue", 45.67);

            EmailNotificationRequest request = EmailNotificationRequest.builder()
                .emailAddress(TEST_EMAIL)
                .templateId(TEMPLATE_ID)
                .personalisation(personalisation)
                .build();

            EmailNotificationResponse expectedResponse = createEmailResponse();

            when(notificationService.scheduleEmailNotification(any(EmailNotificationRequest.class), any(UUID.class)))
                .thenReturn(expectedResponse);

            ResponseEntity<EmailNotificationResponse> response = notifyController.sendEmail(
                AUTH_HEADER, SERVICE_AUTH_HEADER, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
            assertThat(response.getBody()).isNotNull();
            verify(notificationService).scheduleEmailNotification(eq(request), any(UUID.class));
        }

        @Test
        @DisplayName("Should handle null reference and reply-to ID")
        void shouldHandleNullReferenceAndReplyToId() {
            EmailNotificationRequest request = EmailNotificationRequest.builder()
                .emailAddress(TEST_EMAIL)
                .templateId(TEMPLATE_ID)
                .reference(null)
                .emailReplyToId(null)
                .build();

            EmailNotificationResponse expectedResponse = createEmailResponse();

            when(notificationService.scheduleEmailNotification(any(EmailNotificationRequest.class), any(UUID.class)))
                .thenReturn(expectedResponse);

            ResponseEntity<EmailNotificationResponse> response = notifyController.sendEmail(
                AUTH_HEADER, SERVICE_AUTH_HEADER, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
            assertThat(response.getBody()).isNotNull();
            verify(notificationService).scheduleEmailNotification(eq(request), any(UUID.class));
        }
    }

    @Nested
    @DisplayName("Send Defendant Response Emails Tests")
    class SendDefendantResponseEmailsTests {
        @Test
        @DisplayName("Should return all email responses when defendant response exists")
        void shouldReturnAllEmailResponsesWhenDefendantResponseExists() {
            UUID defendantResponseId = UUID.randomUUID();

            PartyEntity party = new PartyEntity();
            party.setEmailAddress(TEST_EMAIL);
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

            when(defendantResponseRepository.findById(defendantResponseId))
                .thenReturn(Optional.of(defendantResponse));

            EmailNotificationResponse response = createEmailResponse();

            when(notificationService.sendDefendantResponseNoCounterclaimEmailNotification(defendantResponse))
                .thenReturn(response);
            when(notificationService.sendDefendantResponseCounterclaimPaymentRequiredEmailNotification(defendantResponse))
                .thenReturn(response);
            when(notificationService.sendDefendantResponseCounterclaimPaymentSuccessEmailNotification(defendantResponse))
                .thenReturn(response);
            when(notificationService.sendDefendantResponseCounterclaimNoPaymentRequiredEmailNotification(defendantResponse))
                .thenReturn(response);

            ResponseEntity<List<EmailNotificationResponse>> result =
                notifyController.sendDefendantResponseEmails(
                    AUTH_HEADER, SERVICE_AUTH_HEADER, defendantResponseId);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).hasSize(4);

            verify(defendantResponseRepository).findById(defendantResponseId);
            verify(notificationService).sendDefendantResponseNoCounterclaimEmailNotification(defendantResponse);
            verify(notificationService).sendDefendantResponseCounterclaimPaymentRequiredEmailNotification(defendantResponse);
            verify(notificationService).sendDefendantResponseCounterclaimPaymentSuccessEmailNotification(defendantResponse);
            verify(notificationService).sendDefendantResponseCounterclaimNoPaymentRequiredEmailNotification(defendantResponse);
        }

        @Test
        @DisplayName("Should return 404 when defendant response not found")
        void shouldReturn404WhenDefendantResponseNotFound() {
            UUID defendantResponseId = UUID.randomUUID();

            when(defendantResponseRepository.findById(defendantResponseId))
                .thenReturn(Optional.empty());

            ResponseEntity<List<EmailNotificationResponse>> result =
                notifyController.sendDefendantResponseEmails(
                    AUTH_HEADER, SERVICE_AUTH_HEADER, defendantResponseId);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(result.getBody()).isNull();

            verify(defendantResponseRepository).findById(defendantResponseId);
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create controller with dependencies")
        void shouldCreateControllerWithNotificationServiceDependency() {
            NotifyController controller = new NotifyController(notificationService, defendantResponseRepository);

            assertThat(controller).isNotNull();
        }
    }

    private EmailNotificationRequest createValidEmailRequest() {
        Map<String, Object> personalisation = new HashMap<>();
        personalisation.put("name", "Test User");
        personalisation.put("reference", "TEST-REF-123");

        return EmailNotificationRequest.builder()
            .emailAddress(TEST_EMAIL)
            .templateId(TEMPLATE_ID)
            .personalisation(personalisation)
            .reference("external-ref-456")
            .emailReplyToId("reply-to-789")
            .build();
    }

    private EmailNotificationResponse createEmailResponse() {
        EmailNotificationResponse response = new EmailNotificationResponse();
        response.setTaskId(TASK_ID);
        response.setStatus(SCHEDULED_STATUS);
        response.setNotificationId(UUID.randomUUID());
        return response;
    }
}
