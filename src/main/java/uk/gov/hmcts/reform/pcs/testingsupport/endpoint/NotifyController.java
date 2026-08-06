package uk.gov.hmcts.reform.pcs.testingsupport.endpoint;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DefendantResponseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.feeandpay.FeePaymentRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.LegalRepresentativeOrganisationRepository;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationResponse;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;

import java.util.List;
import java.util.Optional;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Slf4j
@RestController
@RequestMapping("/testing-support")
@ConditionalOnProperty(name = "testing-support.enabled", havingValue = "true")
public class NotifyController {

    private final NotificationService notificationService;
    private final DefendantResponseRepository defendantResponseRepository;
    private final FeePaymentRepository feePaymentRepository;
    private final LegalRepresentativeOrganisationRepository legalRepresentativeOrganisationRepository;

    public NotifyController(NotificationService notificationService,
                            DefendantResponseRepository defendantResponseRepository,
                            FeePaymentRepository feePaymentRepository,
                            LegalRepresentativeOrganisationRepository legalRepresentativeOrganisationRepository) {
        this.notificationService = notificationService;
        this.defendantResponseRepository = defendantResponseRepository;
        this.feePaymentRepository = feePaymentRepository;
        this.legalRepresentativeOrganisationRepository = legalRepresentativeOrganisationRepository;
    }

    @PostMapping(value = "send-defendant-response-emails")
    public ResponseEntity<List<EmailNotificationResponse>> sendDefendantResponseEmails(
        @RequestHeader(value = AUTHORIZATION, defaultValue = "DummyId") String authorisation,
        @RequestHeader(value = "ServiceAuthorization") String serviceAuthorization,
        @RequestParam Integer defendantResponseId) {

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
            notificationService.sendDefendantResponseCounterclaimPaymentSuccessEmailNotification(
                defendantResponse,
                "PAY-123"
            ),
            notificationService.sendDefendantResponseCounterclaimNoPaymentRequiredEmailNotification(defendantResponse)
        );

        return ResponseEntity.ok(responses);
    }


    @PostMapping(value = "testLegalRepEndPoint")
    public ResponseEntity<List<EmailNotificationResponse>> sendDefendantResponseEmailsToLegalRep(
        @RequestHeader(value = AUTHORIZATION, defaultValue = "DummyId") String authorisation,
        @RequestHeader(value = "ServiceAuthorization") String serviceAuthorization,
        @RequestParam Integer defendantResponseId) {

        //        legalRepSubmissionEventStrategy.process(null);

        log.info("Received request to send all defendant response emails for {}", defendantResponseId);
        // temporary endpoint to test sending emails

        Optional<DefendantResponseEntity> optDefendantResponse =
            defendantResponseRepository.findById(defendantResponseId);

        Optional<LegalRepresentativeOrganisationEntity> optionalLegalRepresentativeOrganisationEntity =
            legalRepresentativeOrganisationRepository.findByPartyLinkedToLegalRepresentativeOrganisationAndActive(
                optDefendantResponse.get().getParty().getId());

        if (optDefendantResponse.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        LegalRepresentativeOrganisationEntity legalRepresentativeOrganisationEntity =
            optionalLegalRepresentativeOrganisationEntity.get();
        DefendantResponseEntity defendantResponse = optDefendantResponse.get();
        List<EmailNotificationResponse> responses = List.of(
            notificationService.sendDefendantResponseCounterclaimToLegalRepresentativePaymentSuccess(
                legalRepresentativeOrganisationEntity,
                "PAY-123",
                defendantResponse.getPcsCase(),
                defendantResponse
            )
        );

        return ResponseEntity.ok(responses);
    }
}
