package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaim;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.notify.model.NotificationClaimType;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;
import uk.gov.hmcts.reform.pcs.notify.template.EmailTemplate;

import java.util.List;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Component
@RequiredArgsConstructor
public class LegalRepHelper {

    private final NotificationService notificationService;

    public void submit(LegalRepresentativeEntity legalRepresentativeEntity,
                       PcsCaseEntity pcsCaseEntity,
                       PartyEntity legalRepresentativePartyEntity,
                       DefendantResponseEntity defendantResponse) {

        EmailTemplate emailTemplate = getEmailTemplate(pcsCaseEntity);
        NotificationClaimType notificationClaimType = getNotificationClaimType(pcsCaseEntity);

        notificationService.sendDefendantResponseConfirmationToLegalRepresentative(
            legalRepresentativeEntity,
            pcsCaseEntity,
            legalRepresentativePartyEntity,
            defendantResponse,
            emailTemplate,
            notificationClaimType);
    }

    private EmailTemplate getEmailTemplate(PcsCaseEntity pcsCaseEntity) {
        // TODO: replace with actual EmailTemplate
        Optional<CounterClaimEntity> counterClaimEntityOptional = getCounterClaim(pcsCaseEntity);

        // NO Counter Claim
        if (counterClaimEntityOptional.isEmpty()){
            return EmailTemplate.RESPONSE_NO_COUNTERCLAIM;
        }

        CounterClaimEntity counterClaimEntity = counterClaimEntityOptional.get();

        //  Counter Claim Issued and blank HWF reference & TO add payment successful -
        if (isCounterClaimIssued(counterClaimEntity) && isHwfBlank(counterClaimEntity)) {
            return EmailTemplate.COUNTERCLAIM_PAYMENT_SUCCESS;
        }
        return EmailTemplate.RESPONSE_WITH_COUNTERCLAIM_NO_PAYMENT_REQUIRED;
    }

    private NotificationClaimType getNotificationClaimType(PcsCaseEntity pcsCaseEntity) {
        // TODO: replace with actual NotificationClaimType
        if (getCounterClaim(pcsCaseEntity).isPresent()) {
            return NotificationClaimType.COUNTER_CLAIM;
        }
        return NotificationClaimType.NO_COUNTER_CLAIM;
    }

    private boolean isCounterClaimIssued(CounterClaimEntity counterClaim) {
        return CounterClaimState.COUNTER_CLAIM_ISSUED == counterClaim.getStatus();
    }

    private boolean isHwfBlank(CounterClaimEntity counterClaim) {
        return isBlank(counterClaim.getHwfReferenceNumber());
    }


    private Optional<CounterClaimEntity> getCounterClaim(PcsCaseEntity pcsCaseEntity) {
        if (pcsCaseEntity == null
            || pcsCaseEntity.getCounterClaims() == null
            || pcsCaseEntity.getCounterClaims().isEmpty() ) {
            return Optional.empty();
        }
    return Optional.of(pcsCaseEntity.getCounterClaims().getFirst());
    }
}
