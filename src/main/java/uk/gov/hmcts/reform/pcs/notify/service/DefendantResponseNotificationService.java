package uk.gov.hmcts.reform.pcs.notify.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    public void sendEmailNotificationForNoCounterClaim(UUID defendantResponseId) {
        DefendantResponseEntity defendantResponse = defendantResponseRepository.findById(defendantResponseId)
            .orElseThrow(() -> new IllegalArgumentException("Defendant response not found: " + defendantResponseId));

        CounterClaimEntity counterClaim = getAssociatedCounterClaim(defendantResponse);
        if (counterClaim != null) {
            log.info("Defendant response {} has a counterclaim, skipping no counter claim email",
                     defendantResponse.getId());
            return;
        }
        notificationService.sendDefendantResponseNoCounterclaimEmailNotification(defendantResponse);
    }

    public void sendEmailNotificationForCounterclaim(UUID defendantResponseId) {
        DefendantResponseEntity defendantResponse = defendantResponseRepository.findById(defendantResponseId)
            .orElseThrow(() -> new IllegalArgumentException("Defendant response not found: " + defendantResponseId));

        CounterClaimEntity counterClaim = getAssociatedCounterClaim(defendantResponse);
        if (counterClaim == null) {
            log.info("Defendant response {} has no counterclaim. Not sending email notification",
                     defendantResponse.getId());
            return;
        }

        boolean isHwfRequested = counterClaim.getNeedHelpWithFees() != null
            && counterClaim.getNeedHelpWithFees().toBoolean();

        boolean hasHwfReference = counterClaim.getHwfReferenceNumber() != null
            && !counterClaim.getHwfReferenceNumber().isBlank();

        if (isHwfRequested && !hasHwfReference) {
            log.info("Not sending email as HWF is requested but reference is blank for defendant response {}",
                     defendantResponse.getId());
            return;
        }

        if (!isHwfRequested && hasHwfReference) {
            log.info("Not sending email as HWF is not requested but reference is not blank for defendant response {}",
                     defendantResponse.getId());
            return;
        }

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
