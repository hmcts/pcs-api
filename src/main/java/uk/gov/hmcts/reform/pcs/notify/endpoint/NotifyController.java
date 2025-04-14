package uk.gov.hmcts.reform.pcs.notify.endpoint;

import lombok.extern.slf4j.Slf4j;
import org.jobrunr.jobs.JobId;
import org.jobrunr.scheduling.BackgroundJob;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationRequest;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

/*
    This is a temporary endpoint created purely for testing the integration with Gov Notify, and will be removed once
    events are added to our service, DO NOT USE for any future events.
*/

@Slf4j
@RestController
@RequestMapping("/notify")
public class NotifyController {

    private final NotificationService notificationService;

    public NotifyController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping(value = "/send-email", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> sendEmail(
        @RequestHeader(value = AUTHORIZATION, defaultValue = "DummyId") String authorisation,
        @RequestHeader(value = "ServiceAuthorization") String serviceAuthorization,
        @RequestBody EmailNotificationRequest emailRequest) {
        final String referenceId = UUID.randomUUID().toString();

        JobId emailSendId = BackgroundJob.enqueue(() -> notificationService.sendEmail(emailRequest, referenceId));

        JobId emailCheckId = BackgroundJob.schedule(
            Instant.now().plus(5, ChronoUnit.SECONDS),
            () -> notificationService.getNotificationStatus("30a69eb3-2ab3-43d4-8067-b0a878bda41b"));

        // USE NOTIFICATION ID YOU MORON

        return ResponseEntity.ok("Jobs enqueued successfully - Email job: "
                                        + ", Status check job: ");
    }
}
