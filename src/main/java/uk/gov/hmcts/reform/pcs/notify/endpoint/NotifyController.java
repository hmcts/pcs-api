package uk.gov.hmcts.reform.pcs.notify.endpoint;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationRequest;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationResponse;
import uk.gov.hmcts.reform.pcs.notify.config.EmailState;
import uk.gov.hmcts.reform.pcs.notify.config.EmailTaskConfiguration;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Slf4j
@RestController
@RequestMapping("/notify")
public class NotifyController {

    private final SchedulerClient schedulerClient;

    public NotifyController(SchedulerClient schedulerClient) {
        this.schedulerClient = schedulerClient;
    }

    @PostMapping(value = "/send-email", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EmailNotificationResponse> sendEmail(
        @RequestHeader(value = AUTHORIZATION, defaultValue = "DummyId") String authorisation,
        @RequestHeader(value = "ServiceAuthorization") String serviceAuthorization,
        @RequestBody EmailNotificationRequest emailRequest) {

        log.info("Received request to send email to: {}", emailRequest.getEmailAddress());

        // Generate a unique ID for this email task
        String taskId = UUID.randomUUID().toString();

        // Create the email state object
        EmailState emailState = new EmailState(
            taskId,
            emailRequest.getEmailAddress(),
            emailRequest.getTemplateId(),
            emailRequest.getPersonalisation(),
            emailRequest.getReference(),
            emailRequest.getEmailReplyToId(),
            null // notification ID will be set after sending
        );

        // Schedule the send email task to run immediately
        schedulerClient.scheduleIfNotExists(
            EmailTaskConfiguration.sendEmailTask
                .instance(taskId)
                .data(emailState)
                .scheduledTo(Instant.now())
        );

        // Create response
        EmailNotificationResponse response = new EmailNotificationResponse();
        response.setTaskId(taskId);
        response.setStatus("SCHEDULED");

        return ResponseEntity.accepted().body(response);
    }
}
