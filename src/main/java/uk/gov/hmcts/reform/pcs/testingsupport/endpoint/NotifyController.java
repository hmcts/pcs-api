package uk.gov.hmcts.reform.pcs.testingsupport.endpoint;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DefendantResponseRepository;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationResponse;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Slf4j
@RestController
@RequestMapping("/testing-support")
@ConditionalOnProperty(name = "testing-support.enabled", havingValue = "true")
public class NotifyController {

    private final NotificationService notificationService;
    private final DefendantResponseRepository defendantResponseRepository;

    public NotifyController(NotificationService notificationService,
                            DefendantResponseRepository defendantResponseRepository) {
        this.notificationService = notificationService;
        this.defendantResponseRepository = defendantResponseRepository;
    }

    @PostMapping(value = "send-defendant-response-emails")
    public ResponseEntity<List<EmailNotificationResponse>> sendDefendantResponseEmails(
        @RequestHeader(value = AUTHORIZATION, defaultValue = "DummyId") String authorisation,
        @RequestHeader(value = "ServiceAuthorization") String serviceAuthorization,
        @RequestParam UUID defendantResponseId) {

        log.info("Received request to send all defendant response emails for {}", defendantResponseId);
        // temporary endpoint to test sending emails

        Optional<DefendantResponseEntity> optDefendantResponse =
            defendantResponseRepository.findById(defendantResponseId);

        if (optDefendantResponse.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        DefendantResponseEntity defendantResponse = optDefendantResponse.get();
        List<EmailNotificationResponse> responses = List.of(
            notificationService.sendDefendantResponseNoCounterclaimEmailNotification(defendantResponse),
            notificationService.sendDefendantResponseCounterclaimPaymentRequiredEmailNotification(defendantResponse),
            notificationService.sendDefendantResponseCounterclaimPaymentSuccessEmailNotification(defendantResponse),
            notificationService.sendDefendantResponseCounterclaimNoPaymentRequiredEmailNotification(defendantResponse)
        );

        return ResponseEntity.ok(responses);
    }
}
