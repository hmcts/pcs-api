package uk.gov.hmcts.reform.pcs.notify.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.notify.service.DefendantResponseNotificationService;
import uk.gov.hmcts.reform.pcs.notify.service.PaymentNotificationService;

@Slf4j
@Service
@RequiredArgsConstructor
public class CounterClaimEventListener {

    private final CounterClaimRepository counterClaimRepository;
    private final DefendantResponseNotificationService defendantResponseNotificationService;
    private final PaymentNotificationService paymentNotificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(CounterClaimStatusUpdatedEvent event) {
        CounterClaimEntity entity = counterClaimRepository.findById(event.getEntityId())
            .orElseThrow(() -> new IllegalArgumentException("Counterclaim not found: " + event.getEntityId()));

        switch (event.getNewStatus()) {
            case PENDING_COUNTER_CLAIM_ISSUED -> handleNotificationForDefendantResponse(entity);
            case COUNTER_CLAIM_ISSUED -> handleCounterClaimIssued(entity);
        }
    }

    private void handleNotificationForDefendantResponse(CounterClaimEntity entity) {
        entity.getPcsCase().getDefendantResponses().stream()
            .filter(dr -> dr.getParty().getId().equals(entity.getParty().getId()))
            .findFirst()
            .map(DefendantResponseEntity::getId)
            .ifPresent(defendantResponseNotificationService::sendEmailNotificationForCounterclaim);
    }

    private void handleCounterClaimIssued(CounterClaimEntity entity) {
        PartyEntity defendant = entity.getParty();
        PcsCaseEntity pcsCase = entity.getPcsCase();

        FeePaymentEntity feePayment = pcsCase.getClaims().stream()
            .filter(claim -> claim.getFeePayment() != null
                && claim.getFeePayment().getParty().getId().equals(defendant.getId()))
            .map(ClaimEntity::getFeePayment)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "No fee payment found for counterclaim: " + entity.getId()));

        if (feePayment.getPaymentStatus() != PaymentStatus.PAID) {
            log.info("Fee payment {} not paid, skipping email notification", feePayment.getId());
            return;
        }
        paymentNotificationService.sendCounterClaimPaymentSuccessNotification(feePayment.getId());
    }
}
