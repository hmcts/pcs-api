package uk.gov.hmcts.reform.pcs.notify.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentNotificationService {

    private final NotificationService notificationService;
    private final CounterClaimRepository counterClaimRepository;

    @Transactional
    public void sendCounterClaimPaymentSuccessNotification(UUID counterClaimId) {
        CounterClaimEntity counterClaim = counterClaimRepository.findById(counterClaimId)
            .orElseThrow(() -> new IllegalArgumentException("Counter claim not found: " + counterClaimId));

        PcsCaseEntity pcsCase = counterClaim.getPcsCase();
        PartyEntity defendant = counterClaim.getParty();

        DefendantResponseEntity defendantResponse = pcsCase.getDefendantResponses().stream()
            .filter(counter -> counter.getParty().getId().equals(defendant.getId()))
            .findFirst()
            .orElse(null);

        if (defendantResponse == null) {
            log.warn("No defendant response found for case reference {}", pcsCase.getCaseReference());
            return;
        }

        log.info("Sending counterclaim payment success email case reference {}", pcsCase.getCaseReference());
        notificationService.sendDefendantResponseCounterclaimPaymentSuccessEmailNotification(defendantResponse);
    }
}
