package uk.gov.hmcts.reform.pcs.notify.listener;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimStatus;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.notify.service.DefendantResponseNotificationService;
import uk.gov.hmcts.reform.pcs.notify.service.PaymentNotificationService;

@Slf4j
@Component
public class CounterClaimEntityListener {

    private final DefendantResponseNotificationService defendantResponseNotificationService;
    private final PaymentNotificationService paymentNotificationService;

    public CounterClaimEntityListener(@Lazy DefendantResponseNotificationService defendantResponseNotificationService,
                                      @Lazy PaymentNotificationService paymentNotificationService) {
        this.defendantResponseNotificationService = defendantResponseNotificationService;
        this.paymentNotificationService = paymentNotificationService;
    }

    @PostLoad
    public void onPostLoad(CounterClaimEntity entity) {
        entity.setPreviousStatus(entity.getStatus());
    }

    @PostPersist
    public void onPostPersist(CounterClaimEntity entity) {
        if (entity.getStatus() == CounterClaimStatus.PENDING_CASE_ISSUED) {
            handleNotificationForDefendantResponse(entity);
        }
    }

    @PostUpdate
    public void onPostUpdate(CounterClaimEntity entity) {
        if (entity.getStatus() == entity.getPreviousStatus()) {
            return;
        }

        switch (entity.getStatus()) {
            case PENDING_CASE_ISSUED -> handleNotificationForDefendantResponse(entity);
            case CASE_ISSUED -> {
                PartyEntity defendant = entity.getParty();
                FeePaymentEntity feePayment = entity.getPcsCase().getClaims().stream()
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
    }

    private void handleNotificationForDefendantResponse(CounterClaimEntity entity) {
        entity.getPcsCase().getDefendantResponses().stream()
            .filter(dr -> dr.getParty().getId().equals(entity.getParty().getId()))
            .findFirst()
            .map(DefendantResponseEntity::getId)
            .ifPresent(defendantResponseNotificationService::sendEmailNotificationForCounterclaim);
    }
}
