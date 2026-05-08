package uk.gov.hmcts.reform.pcs.notify.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.repository.DefendantResponseRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefendantResponseNotificationService {

    private final NotificationService notificationService;
    private final DefendantResponseRepository defendantResponseRepository;

    private static final String PENDING_CASE_ISSUED = "PENDING_CASE_ISSUED";

    @Async
    public void sendEmailNotification(UUID defendantResponseId) {
        DefendantResponseEntity defendantResponse = defendantResponseRepository.findById(defendantResponseId)
            .orElseThrow(() -> new IllegalArgumentException("Defendant response not found: " + defendantResponseId));

        CounterClaimEntity counterClaim = getAssociatedCounterClaim(defendantResponse);
        if (counterClaim == null) {
            log.info("Sending no counter claim email for defendant response {}",
                     defendantResponse.getId());
            notificationService.sendDefendantResponseNoCounterclaimEmailNotification(defendantResponse);
            return;
        }

        if (!PENDING_CASE_ISSUED.equals(counterClaim.getStatus())) {
            log.info("Counterclaim status not eligible for email. status={}, defendantResponseId={}",
                     counterClaim.getStatus(), defendantResponse.getId());
            return;
        }

        boolean hasHwfReference = counterClaim.getHwfReferenceNumber() != null
            && !counterClaim.getHwfReferenceNumber().isBlank();

        if (!hasHwfReference) {
            log.info("Sending counterclaim payment required email for defendant response {}",
                     defendantResponse.getId());
            notificationService.sendDefendantResponseCounterclaimPaymentRequiredEmailNotification(defendantResponse);
            return;
        }

        log.info("Sending counterclaim no payment required email for defendant response {}",
                 defendantResponse.getId());
        notificationService.sendDefendantResponseCounterclaimNoPaymentRequiredEmailNotification(defendantResponse);
    }

    private CounterClaimEntity getAssociatedCounterClaim(DefendantResponseEntity defendantResponse) {
        UUID partyId = defendantResponse.getParty().getId();
        PcsCaseEntity pcsCase = defendantResponse.getPcsCase();

        return pcsCase.getCounterClaims().stream()
            .filter(counterClaim -> counterClaim.getParty().getId().equals(partyId))
            .findFirst()
            .orElse(null);
    }
}
