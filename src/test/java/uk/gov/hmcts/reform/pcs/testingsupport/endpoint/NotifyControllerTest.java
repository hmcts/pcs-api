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
import uk.gov.hmcts.reform.pcs.notify.exception.NotificationException;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationRequest;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationResponse;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotifyController Tests")
class NotifyControllerTest {

    @Mock
    private NotificationService notificationService;

    private NotifyController notifyController;

    private static final String AUTH_HEADER = "Bearer test-token";
    private static final String SERVICE_AUTH_HEADER = "service-auth-token";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEMPLATE_ID = "template-123";
    private static final String TASK_ID = "task-456";
    private static final String SCHEDULED_STATUS = "SCHEDULED";

    @BeforeEach
    void setUp() {
        notifyController = new NotifyController(notificationService);
    }

    @Nested
    @DisplayName("Send Email Tests")
    class SendEmailTests {

        @Test
        @DisplayName("Should successfully schedule email notification")
        void shouldSuccessfullyScheduleEmailNotification() {
            EmailNotificationRequest request = createValidEmailRequest();
            EmailNotificationResponse expectedResponse = createEmailResponse();

            when(notificationService.scheduleEmailNotification(any(EmailNotificationRequest.class)))
                .thenReturn(expectedResponse);

            ResponseEntity<EmailNotificationResponse> response = notifyController.sendEmail(
                AUTH_HEADER, SERVICE_AUTH_HEADER, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getTaskId()).isEqualTo(TASK_ID);
            assertThat(response.getBody().getStatus()).isEqualTo(SCHEDULED_STATUS);

            verify(notificationService).scheduleEmailNotification(request);
        }

        @Test
        @DisplayName("Should handle minimal email request")
        void shouldHandleMinimalEmailRequest() {
            EmailNotificationRequest request = EmailNotificationRequest.builder()
                .emailAddress(TEST_EMAIL)
                .templateId(TEMPLATE_ID)
                .build();

            EmailNotificationResponse expectedResponse = createEmailResponse();

            when(notificationService.scheduleEmailNotification(any(EmailNotificationRequest.class)))
                .thenReturn(expectedResponse);

            ResponseEntity<EmailNotificationResponse> response = notifyController.sendEmail(
                AUTH_HEADER, SERVICE_AUTH_HEADER, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
            assertThat(response.getBody()).isNotNull();
            verify(notificationService).scheduleEmailNotification(request);
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

            when(notificationService.scheduleEmailNotification(any(EmailNotificationRequest.class)))
                .thenReturn(expectedResponse);

            ResponseEntity<EmailNotificationResponse> response = notifyController.sendEmail(
                AUTH_HEADER, SERVICE_AUTH_HEADER, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getTaskId()).isEqualTo(TASK_ID);

            verify(notificationService).scheduleEmailNotification(request);
        }

        @Test
        @DisplayName("Should use default authorization header when not provided")
        void shouldUseDefaultAuthorizationHeaderWhenNotProvided() {
            EmailNotificationRequest request = createValidEmailRequest();
            EmailNotificationResponse expectedResponse = createEmailResponse();

            when(notificationService.scheduleEmailNotification(any(EmailNotificationRequest.class)))
                .thenReturn(expectedResponse);

            ResponseEntity<EmailNotificationResponse> response = notifyController.sendEmail(
                "DummyId", SERVICE_AUTH_HEADER, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
            assertThat(response.getBody()).isNotNull();
            verify(notificationService).scheduleEmailNotification(request);
        }

        @Test
        @DisplayName("Should return internal server error when service throws exception")
        void shouldReturnInternalServerErrorWhenServiceThrowsException() {
            EmailNotificationRequest request = createValidEmailRequest();

            when(notificationService.scheduleEmailNotification(any(EmailNotificationRequest.class)))
                .thenThrow(new NotificationException("Database error", new RuntimeException()));

            ResponseEntity<EmailNotificationResponse> response = notifyController.sendEmail(
                AUTH_HEADER, SERVICE_AUTH_HEADER, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNull();

            verify(notificationService).scheduleEmailNotification(request);
        }

        @Test
        @DisplayName("Should return internal server error when service throws runtime exception")
        void shouldReturnInternalServerErrorWhenServiceThrowsRuntimeException() {
            EmailNotificationRequest request = createValidEmailRequest();

            when(notificationService.scheduleEmailNotification(any(EmailNotificationRequest.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

            ResponseEntity<EmailNotificationResponse> response = notifyController.sendEmail(
                AUTH_HEADER, SERVICE_AUTH_HEADER, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNull();

            verify(notificationService).scheduleEmailNotification(request);
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

            when(notificationService.scheduleEmailNotification(any(EmailNotificationRequest.class)))
                .thenReturn(expectedResponse);

            ResponseEntity<EmailNotificationResponse> response = notifyController.sendEmail(
                AUTH_HEADER, SERVICE_AUTH_HEADER, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
            assertThat(response.getBody()).isNotNull();
            verify(notificationService).scheduleEmailNotification(request);
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

            when(notificationService.scheduleEmailNotification(any(EmailNotificationRequest.class)))
                .thenReturn(expectedResponse);

            ResponseEntity<EmailNotificationResponse> response = notifyController.sendEmail(
                AUTH_HEADER, SERVICE_AUTH_HEADER, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
            assertThat(response.getBody()).isNotNull();
            verify(notificationService).scheduleEmailNotification(request);
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

            when(notificationService.scheduleEmailNotification(any(EmailNotificationRequest.class)))
                .thenReturn(expectedResponse);

            ResponseEntity<EmailNotificationResponse> response = notifyController.sendEmail(
                AUTH_HEADER, SERVICE_AUTH_HEADER, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
            assertThat(response.getBody()).isNotNull();
            verify(notificationService).scheduleEmailNotification(request);
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create controller with notification service dependency")
        void shouldCreateControllerWithNotificationServiceDependency() {
            NotifyController controller = new NotifyController(notificationService);

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
