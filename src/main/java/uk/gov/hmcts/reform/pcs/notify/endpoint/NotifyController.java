package uk.gov.hmcts.reform.pcs.notify.endpoint;

import static uk.gov.hmcts.reform.pcs.notify.constants.NotifyConstants.SERVICE_AUTHORIZATION;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pcs.notify.model.NotificationResponse;
import uk.gov.hmcts.reform.pcs.notify.model.SendEmail;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;

@Slf4j
@RestController
@RequestMapping("/notifications")
public class NotifyController {

    private final NotificationService notificationService;

    @Autowired
    public NotifyController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/send-email")
    public ResponseEntity<NotificationResponse> sendEmail(
        @Valid
        @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorization,
        @RequestBody SendEmail emailRequest) {
        log.info("Received request to send email to {}", emailRequest.getEmailAddress());

        NotificationResponse notificationResponse = notificationService.sendEmail(emailRequest);

        return ResponseEntity.ok(notificationResponse);
    }
}
