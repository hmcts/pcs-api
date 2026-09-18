package uk.gov.hmcts.reform.pcs.notify.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaim;
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.DefendantResponseRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.CounterClaimFeeCalculator;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefendantResponseNotificationService {

    private final NotificationService notificationService;
    private final DefendantResponseRepository defendantResponseRepository;
    private final CounterClaimRepository counterClaimRepository;
    private final CounterClaimFeeCalculator counterClaimFeeCalculator;

    @Transactional
    public void sendDefendantResponseReceived(Integer defendantResponseId) {
        DefendantResponseEntity defendantResponse = defendantResponseRepository.findById(defendantResponseId)
            .orElseThrow(() -> new IllegalArgumentException("Defendant response not found: " + defendantResponseId));

        notificationService.sendClaimantDefendantResponseReceivedEmailNotification(defendantResponse.getClaim());
    }

    @Transactional
    public void sendEmailNotificationForNoCounterClaim(Integer defendantResponseId) {
        DefendantResponseEntity defendantResponse = defendantResponseRepository.findById(defendantResponseId)
            .orElseThrow(() -> new IllegalArgumentException("Defendant response not found: " + defendantResponseId));

        CounterClaimEntity counterClaimEntity = getAssociatedCounterClaim(defendantResponse);
        if (counterClaimEntity != null) {
            log.info("Defendant response {} has a counterclaim, skipping no counter claim email",
                     defendantResponse.getId());
            return;
        }
        notificationService.sendDefendantResponseNoCounterclaimEmailNotification(defendantResponse);
    }

    @Transactional
    public void sendDefendantEmailNotificationForCounterclaim(Integer defendantResponseId) {
        DefendantResponseEntity defendantResponse = defendantResponseRepository.findById(defendantResponseId)
            .orElseThrow(() -> new IllegalArgumentException("Defendant response not found: " + defendantResponseId));

        CounterClaimEntity counterClaimEntity = getAssociatedCounterClaim(defendantResponse);
        if (counterClaimEntity == null) {
            log.info("Defendant response {} has no counterclaim. Not sending email notification",
                     defendantResponse.getId());
            return;
        }

        CounterClaim counterClaim = toCounterClaim(counterClaimEntity);
        if (counterClaimFeeCalculator.isHwfReferencePresent(counterClaim)) {
            log.info("Sending counterclaim no payment required email for defendant response {}",
                     defendantResponse.getId());
            notificationService.sendDefendantResponseCounterclaimNoPaymentRequiredEmailNotification(defendantResponse);
        } else {
            log.info("Sending counterclaim payment required email for defendant response {}",
                     defendantResponse.getId());
            notificationService.sendDefendantResponseCounterclaimPaymentRequiredEmailNotification(defendantResponse);
        }
    }

    @Transactional
    public void sendPendingCounterClaimIssuedNotification(UUID counterClaimId) {
        CounterClaimEntity counterClaimEntity = counterClaimRepository.findById(counterClaimId)
            .orElseThrow(() -> new IllegalArgumentException("Counter claim not found: " + counterClaimId));

        counterClaimEntity.getPcsCase().getDefendantResponses().stream()
            .filter(dr -> dr.getParty().getId().equals(counterClaimEntity.getParty().getId()))
            .findFirst()
            .map(DefendantResponseEntity::getId)
            .ifPresent(this::sendDefendantEmailNotificationForCounterclaim);
    }

    private CounterClaimEntity getAssociatedCounterClaim(DefendantResponseEntity defendantResponse) {
        UUID partyId = defendantResponse.getParty().getId();
        PcsCaseEntity pcsCase = defendantResponse.getPcsCase();

        return pcsCase.getCounterClaims().stream()
            .filter(counterClaim -> counterClaim.getParty().getId().equals(partyId))
            .findFirst()
            .orElse(null);
    }

    public CounterClaim toCounterClaim(CounterClaimEntity entity) {
        return CounterClaim.builder()
            .claimType(entity.getClaimType())
            .isClaimAmountKnown(entity.getIsClaimAmountKnown())
            .claimAmount(entity.getClaimAmount())
            .estimatedMaxClaimAmount(entity.getEstimatedMaxClaimAmount())
            .needHelpWithFees(entity.getNeedHelpWithFees())
            .appliedForHwf(entity.getAppliedForHwf())
            .hwfReferenceNumber(entity.getHwfReferenceNumber())
            .counterClaimFor(entity.getCounterClaimFor())
            .counterClaimReasons(entity.getCounterClaimReasons())
            .otherOrderRequestDetails(entity.getOtherOrderRequestDetails())
            .otherOrderRequestFacts(entity.getOtherOrderRequestFacts())
            .build();
    }
}
