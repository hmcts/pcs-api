package uk.gov.hmcts.reform.pcs.testingsupport.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.reform.pcs.idam.IdamService;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationRequest;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationResponse;
import uk.gov.hmcts.reform.pcs.notify.model.NotificationStatus;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotifyController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotifyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SchedulerClient schedulerClient;

    @Autowired
    private ObjectMapper objectMapper;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public SchedulerClient schedulerClient() {
            return Mockito.mock(SchedulerClient.class);
        }

        @Bean
        public IdamService idamService() {
            return Mockito.mock(IdamService.class);
        }
    }

    @Test
    void shouldScheduleEmailSuccessfully() throws Exception {
        EmailNotificationRequest request = EmailNotificationRequest.builder()
            .emailAddress("test@example.com")
            .templateId("template123")
            .personalisation(new HashMap<>())
            .reference("ref123")
            .emailReplyToId("replyTo123")
            .build();

        when(schedulerClient.scheduleIfNotExists(Mockito.any())).thenReturn(true);

        MvcResult result = mockMvc.perform(post("/notify/send-email")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .header("Authorization", "DummyId")
                                                .header("ServiceAuthorization", "ServiceAuth123")
                                                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.taskId").isNotEmpty())
            .andExpect(jsonPath("$.status").value(NotificationStatus.SCHEDULE.toString()))
            .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        EmailNotificationResponse response = objectMapper.readValue(responseBody, EmailNotificationResponse.class);

        assertThat(response.getTaskId()).isNotBlank();
        assertThat(response.getStatus()).isEqualTo(NotificationStatus.SCHEDULE.toString());
    }

    @Test
    void shouldFailWhenRequiredHeadersAreMissing() throws Exception {
        MvcResult result = mockMvc.perform(post("/notify/send-email")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("{\"emailAddress\":\"test@example.com\","
                                                            + "\"templateId\":\"template123\","
                                                            + "\"personalisation\":{},\"reference\":\"ref123\""
                                                            + ",\"emailReplyToId\":\"replyTo123\"}"))
            .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    void shouldFailWhenRequestBodyIsInvalid() throws Exception {
        MvcResult result = mockMvc.perform(post("/notify/send-email")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .header("Authorization", "DummyId")
                                                .header("ServiceAuthorization", "ServiceAuth123")
                                                .content(""))
            .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
    }
}
