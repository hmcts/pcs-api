package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimSubmission;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenPossessionClaim;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.exception.SecurityContextException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

/**
 * Gives the citizen who made a claim the claim fee's service request, so the pay step can be resumed.
 * Only citizen-created cases have a claim-creator party with an IDAM id.
 */
@Component
@AllArgsConstructor
public class CitizenPossessionClaimView {

    private final FeatureToggleService featureToggleService;
    private final SecurityContextService securityContextService;

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        Optional<String> creatorIdamId = pcsCaseEntity.getParties().stream()
            .filter(PartyEntity::isClaimCreator)
            .map(PartyEntity::getIdamId)
            .filter(Objects::nonNull)
            .findFirst();

        if (creatorIdamId.isEmpty() || pcsCaseEntity.getClaims().isEmpty()
            || !featureToggleService.isEnabled(FeatureFlag.PRIVATE_LANDLORD_MAKE_A_CLAIM)
            || !creatorIdamId.get().equals(currentUserId())) {
            return;
        }

        ClaimEntity mainClaim = pcsCaseEntity.getClaims().getFirst();
        mainClaim.getFeePayments().stream()
            .filter(payment -> payment.getPaymentCallbackHandlerType() == PaymentCallbackHandlerType.CLAIM)
            .filter(payment -> payment.getServiceRequestReference() != null)
            .max(Comparator.comparing(FeePaymentEntity::getId))
            .ifPresent(payment -> pcsCase.setCitizenPossessionClaim(
                CitizenPossessionClaim.builder()
                    .submission(CitizenClaimSubmission.builder()
                                    .serviceRequestReference(payment.getServiceRequestReference())
                                    .feeAmount(payment.getAmount())
                                    .paymentStatus(payment.getPaymentStatus() != null
                                                       ? payment.getPaymentStatus().name() : null)
                                    .build())
                    .build()));
    }

    private String currentUserId() {
        try {
            return securityContextService.getCurrentUserId();
        } catch (SecurityContextException e) {
            return null;
        }
    }

}
