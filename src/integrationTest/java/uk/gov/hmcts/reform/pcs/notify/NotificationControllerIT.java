package uk.gov.hmcts.reform.pcs.notify;



import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.hmcts.reform.pcs.notify.domain.CaseNotification;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationRequest;
import uk.gov.hmcts.reform.pcs.notify.repository.NotificationRepository;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.pcs.hearings.constants.HearingConstants.SERVICE_AUTHORIZATION;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("integration")
public class NotificationControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
        .withDatabaseName("testdb")
        .withUsername("testuser")
        .withPassword("testpass");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }

    private static final String AUTH_HEADER = "Bearer token";
    private static final String SERVICE_AUTH_HEADER = "ServiceAuthToken";
    private static final String END_POINT = "/notify/send-email";

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

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
    }

    @Test
    void testHttpOkWhenEmailIsSentSuccessfully() throws Exception {
        EmailNotificationRequest request = createEmailNotificationRequest();

        mockMvc.perform(post(END_POINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(AUTHORIZATION, AUTH_HEADER)
                            .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                            .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(notificationClient).sendEmail(anyString(), anyString(), anyMap(), anyString());
    }

    @Test
    void testSavingNotificationWhenEndpointGetsCalled() throws Exception {
        EmailNotificationRequest request = createEmailNotificationRequest();

        mockMvc.perform(post(END_POINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(AUTHORIZATION, AUTH_HEADER)
                            .header(SERVICE_AUTHORIZATION,SERVICE_AUTH_HEADER)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        List<CaseNotification> notifications = notificationRepository.findAll();
        assertThat(notifications).hasSize(1);
        assertThat(notifications.getFirst().getRecipient()).isEqualTo(request.getEmailAddress());
        assertThat(notifications.getFirst().getStatus()).isEqualTo("Schedule Pending");
    }

    @Test
    void testBadRequestWhenSendingEmailFails() throws Exception {
        EmailNotificationRequest request = createEmailNotificationRequest();

        when(notificationService.sendEmail(request))
            .thenThrow(new NotificationClientException("Email sending failed"));

        mockMvc.perform(post(END_POINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(AUTHORIZATION, AUTH_HEADER)
                        .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testBadRequestWhenServiceAuthorizationTokenIsMissing() throws Exception {
        mockMvc.perform(post(END_POINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(AUTHORIZATION, AUTH_HEADER)
                            .content(objectMapper.writeValueAsString(mock(EmailNotificationRequest.class))))
            .andExpect(status().isBadRequest());
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


