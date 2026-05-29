package uk.gov.hmcts.reform.pcs.notify.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;

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

        FeePaymentEntity feePayment = pcsCase.getClaims().stream()
            .filter(claim -> claim.getFeePayment() != null
                && claim.getFeePayment().getParty().getId().equals(defendant.getId()))
            .map(ClaimEntity::getFeePayment)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "No fee payment found for counterclaim: " + counterClaim.getId()));

        if (feePayment.getPaymentStatus() != PaymentStatus.PAID) {
            log.info("Fee payment {} not paid, skipping email notification", feePayment.getId());
            return;
        }

        ClaimEntity claim = feePayment.getClaim();

        DefendantResponseEntity defendantResponse = claim.getPcsCase().getDefendantResponses().stream()
            .filter(counter -> counter.getParty().getId().equals(defendant.getId()))
            .findFirst()
            .orElse(null);

        if (defendantResponse == null) {
            log.warn("No defendant response found for claim {}", claim.getId());
            return;
        }

        log.info("Sending counterclaim payment success email for claim {}", claim.getId());
        notificationService.sendDefendantResponseCounterclaimPaymentSuccessEmailNotification(defendantResponse);
    }
}
