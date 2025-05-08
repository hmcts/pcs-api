package uk.gov.hmcts.reform.pcs.notify;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.notify.domain.CaseNotification;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationRequest;
import uk.gov.hmcts.reform.pcs.notify.model.NotificationStatus;
import uk.gov.hmcts.reform.pcs.notify.repository.NotificationRepository;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;
import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.pcs.hearings.constants.HearingConstants.SERVICE_AUTHORIZATION;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("integration")
public class NotificationEndpointIT extends AbstractPostgresContainerIT {

    private static final String AUTH_HEADER = "Bearer token";
    private static final String SERVICE_AUTH_HEADER = "ServiceAuthToken";
    private static final String END_POINT = "/testing-support/send-email";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private NotificationService notificationService;

    @MockitoBean
    private NotificationClient notificationClient;
    
    private UUID mockNotificationId;
    private Notification defaultNotification;

    @BeforeEach
    void setUp() throws NotificationClientException {
        notificationRepository.deleteAll();

        mockNotificationId = UUID.randomUUID();
        SendEmailResponse mockResponse = mock(SendEmailResponse.class);
        when(mockResponse.getNotificationId()).thenReturn(mockNotificationId);
        when(mockResponse.getReference()).thenReturn(Optional.of("reference"));

        when(notificationClient.sendEmail(
            anyString(), anyString(), anyMap(), anyString()
        )).thenReturn(mockResponse);
        
        defaultNotification = mock(Notification.class);
        when(defaultNotification.getStatus()).thenReturn("delivered");
        when(notificationClient.getNotificationById(mockNotificationId.toString())).thenReturn(defaultNotification);
    }

    @DisplayName("Should return Http status Ok when email is sent successfully")
    @Test
    void shouldReturnOkStatusWhenEmailIsSentSuccessfully() throws Exception {
        EmailNotificationRequest request = createEmailNotificationRequest();

        mockMvc.perform(post(END_POINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(AUTHORIZATION, AUTH_HEADER)
                            .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                            .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(notificationClient).sendEmail(
            eq(request.getTemplateId()),
            eq(request.getEmailAddress()),
            anyMap(),
            anyString()
        );
    }

    @DisplayName("Should save case notification when endpoint is called successfully")
    @Test
    void shouldSaveNotificationWhenEndpointGetsCalled() throws Exception {
        EmailNotificationRequest request = createEmailNotificationRequest();

        mockMvc.perform(post(END_POINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(AUTHORIZATION, AUTH_HEADER)
                            .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        List<CaseNotification> notifications = notificationRepository.findAll();
        assertThat(notifications).isNotEmpty();
        assertThat(notifications.getFirst().getRecipient()).isEqualTo(request.getEmailAddress());
        
        assertThat(notifications.getFirst().getStatus()).isEqualTo("delivered");
    }

    @DisplayName("Should return Internal Server Error when sending email fails")
    @Test
    void shouldReturnInternalServerErrorWhenSendingEmailFails() throws Exception {
        EmailNotificationRequest request = createEmailNotificationRequest();

        when(notificationClient.sendEmail(
            anyString(), anyString(), anyMap(), anyString()
        )).thenThrow(new NotificationClientException("Email sending failed"));

        mockMvc.perform(post(END_POINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(AUTHORIZATION, AUTH_HEADER)
                            .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isInternalServerError());
    }

    @DisplayName("Should return bad request when service authorization token is missing")
    @Test
    void shouldReturnBadRequestWithoutServiceAuthorizationToken() throws Exception {
        EmailNotificationRequest request = createEmailNotificationRequest();

        mockMvc.perform(post(END_POINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(AUTHORIZATION, AUTH_HEADER)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
    
    @DisplayName("Should update status to 'delivered' after status check when delivery is successful")
    @Test
    void shouldUpdateStatusToDeliveredWhenDeliveryIsSuccessful() throws Exception {
        Notification notification = mock(Notification.class);
        when(notification.getStatus()).thenReturn("delivered");
        when(notificationClient.getNotificationById(mockNotificationId.toString())).thenReturn(notification);
        
        EmailNotificationRequest request = createEmailNotificationRequest();
        mockMvc.perform(post(END_POINT)
                .contentType(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, AUTH_HEADER)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());
        
        await().atMost(5, TimeUnit.SECONDS).pollInterval(Duration.ofMillis(200)).untilAsserted(() -> {
            List<CaseNotification> notifications = notificationRepository.findAll();
            assertThat(notifications).isNotEmpty();
            assertThat(notifications.getFirst().getStatus()).isEqualTo("delivered");
        });
        
        verify(notificationClient, atLeast(1)).getNotificationById(mockNotificationId.toString());
    }

    @DisplayName("Should update status to 'failed' after status check when delivery fails")
    @Test
    void shouldUpdateStatusToFailedWhenDeliveryFails() throws Exception {
        Notification notification = mock(Notification.class);
        when(notification.getStatus()).thenReturn("failed");
        when(notificationClient.getNotificationById(mockNotificationId.toString())).thenReturn(notification);
        
        EmailNotificationRequest request = createEmailNotificationRequest();
        mockMvc.perform(post(END_POINT)
                .contentType(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, AUTH_HEADER)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());
        
        await().atMost(5, TimeUnit.SECONDS).pollInterval(Duration.ofMillis(200)).untilAsserted(() -> {
            List<CaseNotification> notifications = notificationRepository.findAll();
            assertThat(notifications).isNotEmpty();
            assertThat(notifications.getFirst().getStatus()).isEqualTo("failed");
        });
        
        verify(notificationClient, atLeast(1)).getNotificationById(mockNotificationId.toString());
    }

    @DisplayName("Should retry checking status for 'pending' notifications")
    @Test
    void shouldRetryCheckingStatusForPendingNotifications() throws Exception {
        Notification pendingNotification = mock(Notification.class);
        when(pendingNotification.getStatus()).thenReturn("pending");
        
        Notification deliveredNotification = mock(Notification.class);
        when(deliveredNotification.getStatus()).thenReturn("delivered");
        
        when(notificationClient.getNotificationById(mockNotificationId.toString()))
            .thenReturn(pendingNotification)
            .thenReturn(pendingNotification)
            .thenReturn(deliveredNotification);
        
        EmailNotificationRequest request = createEmailNotificationRequest();
        mockMvc.perform(post(END_POINT)
                .contentType(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, AUTH_HEADER)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());
        
        await().atMost(10, TimeUnit.SECONDS).pollInterval(Duration.ofSeconds(1)).untilAsserted(() -> {
            List<CaseNotification> notifications = notificationRepository.findAll();
            assertThat(notifications).isNotEmpty();
            assertThat(notifications.getFirst().getStatus()).isEqualTo("delivered");
        });
        
        verify(notificationClient, atLeast(3)).getNotificationById(mockNotificationId.toString());
    }

    @DisplayName("Should handle exceptions during status checking")
    @Test
    void shouldHandleExceptionsDuringStatusChecking() throws Exception {
        when(notificationClient.getNotificationById(mockNotificationId.toString()))
            .thenThrow(new NotificationClientException("Status check failed"));
        
        EmailNotificationRequest request = createEmailNotificationRequest();
        mockMvc.perform(post(END_POINT)
                .contentType(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, AUTH_HEADER)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());
        
        await().atMost(5, TimeUnit.SECONDS).pollInterval(Duration.ofMillis(200)).untilAsserted(() -> {
            List<CaseNotification> notifications = notificationRepository.findAll();
            assertThat(notifications).isNotEmpty();
            
            CaseNotification notification = notifications.getFirst();
            assertThat(notification.getProviderNotificationId()).isEqualTo(mockNotificationId);
        });
    }

    private EmailNotificationRequest createEmailNotificationRequest() {
        EmailNotificationRequest request = new EmailNotificationRequest();
        request.setEmailAddress("test@test.com");
        request.setReference(UUID.randomUUID().toString());
        request.setTemplateId("template-id");
        request.setPersonalisation(new HashMap<>());
        return request;
    }
}
