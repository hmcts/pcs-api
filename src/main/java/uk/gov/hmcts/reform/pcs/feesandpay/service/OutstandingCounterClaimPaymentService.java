package uk.gov.hmcts.reform.pcs.feesandpay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimType;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.feeandpay.FeePaymentRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.DefendantAccessValidator;
import uk.gov.hmcts.reform.pcs.exception.FeePaymentNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.OutstandingCounterClaimPayment;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolves outstanding (unpaid) counterclaim fee payment details for a defendant.
 * Used by the citizen dashboard notification and fee-screen resume API.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OutstandingCounterClaimPaymentService {

    private final CounterClaimRepository counterClaimRepository;
    private final FeePaymentRepository feePaymentRepository;
    private final PcsCaseService pcsCaseService;
    private final DefendantAccessValidator defendantAccessValidator;

    @Transactional(readOnly = true)
    public Optional<OutstandingCounterClaimPayment> findOutstandingPaymentForParty(long caseReference, UUID partyId) {
        if (partyId == null) {
            return Optional.empty();
        }

        return counterClaimRepository
            .findFirstByPcsCaseCaseReferenceAndPartyIdAndStatusOrderByClaimSubmittedDateDesc(
                caseReference,
                partyId,
                CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED
            )
            .filter(this::isPaymentRequired)
            .flatMap(this::toOutstandingPayment);
    }

    @Transactional(readOnly = true)
    public OutstandingCounterClaimPayment getOutstandingForDefendant(long caseReference, UUID idamUserId) {
        PcsCaseEntity caseEntity = pcsCaseService.loadCase(caseReference);
        PartyEntity defendant = defendantAccessValidator.validateAndGetDefendant(caseEntity, idamUserId);

        return findOutstandingPaymentForParty(caseReference, defendant.getId())
            .orElseThrow(() -> new FeePaymentNotFoundException(
                "No outstanding counterclaim payment found for case " + caseReference
            ));
    }

    private Optional<OutstandingCounterClaimPayment> toOutstandingPayment(CounterClaimEntity counterClaim) {
        return feePaymentRepository.findByRelatedEntityId(counterClaim.getId())
            .filter(this::hasPayableServiceRequest)
            .map(feePayment -> OutstandingCounterClaimPayment.builder()
                .serviceRequestReference(feePayment.getServiceRequestReference())
                .feeAmount(feePayment.getAmount())
                .counterClaimAmountInPence(toCounterClaimAmountInPence(counterClaim))
                .counterClaimType(toCounterClaimType(counterClaim))
                .build());
    }

    private static String toCounterClaimType(CounterClaimEntity counterClaim) {
        CounterClaimType claimType = counterClaim.getClaimType();
        return claimType != null ? claimType.name() : null;
    }

    private static String toCounterClaimAmountInPence(CounterClaimEntity counterClaim) {
        if (counterClaim.getClaimType() == CounterClaimType.SOMETHING_ELSE) {
            return null;
        }

        BigDecimal amountInPounds = null;
        if (counterClaim.getIsClaimAmountKnown() == VerticalYesNo.YES) {
            amountInPounds = counterClaim.getClaimAmount();
        } else if (counterClaim.getIsClaimAmountKnown() == VerticalYesNo.NO) {
            amountInPounds = counterClaim.getEstimatedMaxClaimAmount();
        }

        if (amountInPounds == null) {
            return null;
        }

        return amountInPounds.movePointRight(2).toPlainString();
    }

    private boolean isPaymentRequired(CounterClaimEntity counterClaim) {
        return !StringUtils.hasText(counterClaim.getHwfReferenceNumber());
    }

    private boolean hasPayableServiceRequest(FeePaymentEntity feePayment) {
        String serviceRequestReference = feePayment.getServiceRequestReference();
        return serviceRequestReference != null
            && !serviceRequestReference.isBlank()
            && feePayment.getAmount() != null;
    }
}
