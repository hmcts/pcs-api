package uk.gov.hmcts.reform.pcs.testingsupport.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.config.TestBeansConfig;
import uk.gov.hmcts.reform.pcs.notify.exception.NotificationException;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationRequest;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationResponse;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;
import uk.gov.hmcts.reform.pcs.util.IdamHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@DisplayName("NotifyController Integration Tests")
@Import(TestBeansConfig.class)
class NotifyControllerIT extends AbstractPostgresContainerIT {

    private static final String SCHEDULED_STATUS = "SCHEDULED";
    private static final String TEST_EMAIL_ADDRESS = "test@example.com";
    private static final String TEMPLATE_123_ID = "template-123";
    private static final String JSON_PATH_STATUS = "$.status";
    private static final String JSON_PATH_TASK_ID = "$.taskId";
    private static final String JSON_PATH_NOTIFICATION_ID = "$.notificationId";
    private static final int ACCEPTED_STATUS = 202;

    private static final String AUTH_HEADER = "Bearer token";
    private static final String SERVICE_AUTH_HEADER = "ServiceAuthToken";
    private static final String SYSTEM_USER_ID_TOKEN = "system-user-id-token";
    private static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    private static final String SEND_EMAIL_ENDPOINT = "/testing-support/send-email";

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @MockitoBean
    private AuthTokenGenerator authTokenGenerator;
    @MockitoBean
    private IdamClient idamClient;
    @MockitoBean
    private NotificationService notificationService;

    public NotifyControllerIT(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @BeforeEach
    void setUp() {
        IdamHelper.stubIdamSystemUser(idamClient, SYSTEM_USER_ID_TOKEN);

        EmailNotificationResponse mockResponse = new EmailNotificationResponse();
        mockResponse.setTaskId("task-123");
        mockResponse.setStatus(SCHEDULED_STATUS);
        mockResponse.setNotificationId(UUID.randomUUID());

        when(notificationService.scheduleEmailNotification(any(EmailNotificationRequest.class)))
            .thenReturn(mockResponse);
    }

    @Nested
    @DisplayName("Successful Email Scheduling Tests")
    class SuccessfulEmailSchedulingTests {

        @Test
        @DisplayName("Should send email with valid request")
        void shouldSendEmailWithValidRequest() throws Exception {
            EmailNotificationRequest request = createValidEmailRequest();

            mockMvc.perform(post(SEND_EMAIL_ENDPOINT)
                                .header(AUTHORIZATION, AUTH_HEADER)
                                .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(ACCEPTED_STATUS))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath(JSON_PATH_TASK_ID, is(notNullValue())))
                .andExpect(jsonPath(JSON_PATH_STATUS, is(SCHEDULED_STATUS)))
                .andExpect(jsonPath(JSON_PATH_NOTIFICATION_ID, is(notNullValue())));
        }

        @Test
        @DisplayName("Should send email with personalisation data")
        void shouldSendEmailWithPersonalisationData() throws Exception {
            Map<String, Object> personalisation = new HashMap<>();
            personalisation.put("firstName", "John");
            personalisation.put("lastName", "Doe");
            personalisation.put("caseReference", "CASE-123");

            EmailNotificationRequest request = EmailNotificationRequest.builder()
                .emailAddress("john.doe@example.com")
                .templateId("template-456")
                .personalisation(personalisation)
                .reference("REF-789")
                .emailReplyToId("reply-to-123")
                .build();

            mockMvc.perform(post(SEND_EMAIL_ENDPOINT)
                                .header(AUTHORIZATION, AUTH_HEADER)
                                .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(ACCEPTED_STATUS))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath(JSON_PATH_TASK_ID, is(notNullValue())))
                .andExpect(jsonPath(JSON_PATH_STATUS, is(SCHEDULED_STATUS)));
        }

        @Test
        @DisplayName("Should send email with empty personalisation")
        void shouldSendEmailWithEmptyPersonalisation() throws Exception {
            EmailNotificationRequest request = EmailNotificationRequest.builder()
                .emailAddress(TEST_EMAIL_ADDRESS)
                .templateId(TEMPLATE_123_ID)
                .personalisation(new HashMap<>())
                .build();

            mockMvc.perform(post(SEND_EMAIL_ENDPOINT)
                                .header(AUTHORIZATION, AUTH_HEADER)
                                .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(ACCEPTED_STATUS))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath(JSON_PATH_TASK_ID, is(notNullValue())))
                .andExpect(jsonPath(JSON_PATH_STATUS, is(SCHEDULED_STATUS)));
        }

        @Test
        @DisplayName("Should send email with special characters in personalisation")
        void shouldSendEmailWithSpecialCharactersInPersonalisation() throws Exception {
            Map<String, Object> personalisation = new HashMap<>();
            personalisation.put("specialChars", "àáâãäåæçèéêë");
            personalisation.put("numbers", 12345);
            personalisation.put("boolean", true);

            EmailNotificationRequest request = EmailNotificationRequest.builder()
                .emailAddress(TEST_EMAIL_ADDRESS)
                .templateId(TEMPLATE_123_ID)
                .personalisation(personalisation)
                .build();

            mockMvc.perform(post(SEND_EMAIL_ENDPOINT)
                                .header(AUTHORIZATION, AUTH_HEADER)
                                .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(ACCEPTED_STATUS))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath(JSON_PATH_TASK_ID, is(notNullValue())))
                .andExpect(jsonPath(JSON_PATH_STATUS, is(SCHEDULED_STATUS)));
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should return 500 when service throws exception")
        void shouldReturn500WhenServiceThrowsException() throws Exception {
            when(notificationService.scheduleEmailNotification(any(EmailNotificationRequest.class)))
                .thenThrow(new NotificationException("Database error", new RuntimeException()));

            EmailNotificationRequest request = createValidEmailRequest();

            mockMvc.perform(post(SEND_EMAIL_ENDPOINT)
                                .header(AUTHORIZATION, AUTH_HEADER)
                                .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("Should return 500 when service throws runtime exception")
        void shouldReturn500WhenServiceThrowsRuntimeException() throws Exception {
            when(notificationService.scheduleEmailNotification(any(EmailNotificationRequest.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

            EmailNotificationRequest request = createValidEmailRequest();

            mockMvc.perform(post(SEND_EMAIL_ENDPOINT)
                                .header(AUTHORIZATION, AUTH_HEADER)
                                .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("Concurrent Request Tests")
    class ConcurrentRequestTests {

        @Test
        @DisplayName("Should handle multiple concurrent requests")
        void shouldHandleMultipleConcurrentRequests() throws Exception {
            EmailNotificationRequest request1 = EmailNotificationRequest.builder()
                .emailAddress("user1@example.com")
                .templateId("template-1")
                .build();

            EmailNotificationRequest request2 = EmailNotificationRequest.builder()
                .emailAddress("user2@example.com")
                .templateId("template-2")
                .build();

            mockMvc.perform(post(SEND_EMAIL_ENDPOINT)
                                .header(AUTHORIZATION, AUTH_HEADER)
                                .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().is(ACCEPTED_STATUS))
                .andExpect(jsonPath(JSON_PATH_TASK_ID, is(notNullValue())))
                .andExpect(jsonPath(JSON_PATH_STATUS, is(SCHEDULED_STATUS)));

            mockMvc.perform(post(SEND_EMAIL_ENDPOINT)
                                .header(AUTHORIZATION, AUTH_HEADER)
                                .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().is(ACCEPTED_STATUS))
                .andExpect(jsonPath(JSON_PATH_TASK_ID, is(notNullValue())))
                .andExpect(jsonPath(JSON_PATH_STATUS, is(SCHEDULED_STATUS)));
        }
    }

    @Nested
    @DisplayName("Request Validation Tests")
    class RequestValidationTests {
        @Test
        @DisplayName("Should handle large personalisation payload")
        void shouldHandleLargePersonalisationPayload() throws Exception {
            Map<String, Object> personalisation = new HashMap<>();
            for (int i = 0; i < 100; i++) {
                personalisation.put("key" + i, "value" + i + " with some longer content to make it bigger");
            }

            EmailNotificationRequest request = EmailNotificationRequest.builder()
                .emailAddress(TEST_EMAIL_ADDRESS)
                .templateId(TEMPLATE_123_ID)
                .personalisation(personalisation)
                .build();

            mockMvc.perform(post(SEND_EMAIL_ENDPOINT)
                                .header(AUTHORIZATION, AUTH_HEADER)
                                .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(ACCEPTED_STATUS))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath(JSON_PATH_TASK_ID, is(notNullValue())))
                .andExpect(jsonPath(JSON_PATH_STATUS, is(SCHEDULED_STATUS)));
        }

        @Test
        @DisplayName("Should handle request with complex personalisation data types")
        void shouldHandleRequestWithComplexPersonalisationDataTypes() throws Exception {
            Map<String, Object> personalisation = new HashMap<>();
            personalisation.put("string", "test value");
            personalisation.put("integer", 42);
            personalisation.put("double", 3.14159);
            personalisation.put("boolean", true);
            personalisation.put("null", null);

            EmailNotificationRequest request = EmailNotificationRequest.builder()
                .emailAddress(TEST_EMAIL_ADDRESS)
                .templateId(TEMPLATE_123_ID)
                .personalisation(personalisation)
                .build();

            mockMvc.perform(post(SEND_EMAIL_ENDPOINT)
                                .header(AUTHORIZATION, AUTH_HEADER)
                                .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(ACCEPTED_STATUS))
                .andExpect(jsonPath(JSON_PATH_TASK_ID, is(notNullValue())))
                .andExpect(jsonPath(JSON_PATH_STATUS, is(SCHEDULED_STATUS)));
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
