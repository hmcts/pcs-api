package uk.gov.hmcts.reform.pcs.notify.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.feeandpay.FeePaymentRepository;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentNotificationService {

    private final NotificationService notificationService;
    private final FeePaymentRepository feePaymentRepository;

    private static final String CASE_ISSUED = "CASE_ISSUED";

    public void sendCounterClaimPaymentSuccessNotification(UUID feePaymentId) {
        FeePaymentEntity feePayment = feePaymentRepository.findById(feePaymentId)
            .orElseThrow(() -> new IllegalArgumentException("Fee payment not found: " + feePaymentId));

        ClaimEntity claim = feePayment.getClaim();
        PartyEntity defendant = claim.getClaimParties()
            .stream()
            .filter(cp -> cp.getRole() == PartyRole.DEFENDANT)
            .map(ClaimPartyEntity::getParty)
            .findFirst()
            .orElse(null);

        if (defendant == null) {
            log.warn("No defendant found for claim {}", claim.getId());
            return;
        }

        CounterClaimEntity counterClaim = claim.getPcsCase().getCounterClaims()
            .stream()
            .filter(counter -> counter.getParty().getId().equals(defendant.getId()))
            .findFirst()
            .orElse(null);

        if (counterClaim == null) {
            log.warn("No counterclaim found for claim {}", claim.getId());
            return;
        }

        DefendantResponseEntity defendantResponse = claim.getPcsCase().getDefendantResponses().stream()
            .filter(counter -> counter.getParty().getId().equals(defendant.getId()))
            .findFirst()
            .orElse(null);

        if (defendantResponse == null) {
            log.warn("No defendant response found for claim {}", claim.getId());
            return;
        }

        if (!CASE_ISSUED.equals(counterClaim.getStatus())) {
            log.info("Counterclaim status not eligible for AC04 email: {}", counterClaim.getStatus());
            return;
        }

        log.info("Sending counterclaim payment success email for claim {}", claim.getId());
        notificationService.sendDefendantResponseCounterclaimPaymentSuccessEmailNotification(defendantResponse);
    }
}
