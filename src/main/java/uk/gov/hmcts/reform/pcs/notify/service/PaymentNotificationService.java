package uk.gov.hmcts.reform.pcs.notify.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.LegalRepresentativeOrganisationRepository;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentNotificationService {

    private final NotificationService notificationService;
    private final CounterClaimRepository counterClaimRepository;
    private final SecurityContextService securityContextService;
    private final LegalRepresentativeOrganisationRepository legalRepresentativeOrganisationRepository;

    @Transactional
    public void sendCounterClaimPaymentSuccessNotification(UUID counterClaimId, String paymentReference) {
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

        UUID userUUID = securityContextService.getCurrentUserId();
        LegalRepresentativeOrganisationEntity legalRepresentativeOrganisationEntity =
            legalRepresentativeOrganisationRepository.findByPartyLinkedToLegalRepresentativeOrganisationAndActive(
                defendantResponse.getParty().getId()).orElse(null);

        log.info("Sending counterclaim payment success email case reference {}", pcsCase.getCaseReference());

        if (Objects.equals(userUUID, defendant.getIdamId())) {
            log.info("Sending counterclaim payment success email case reference {}", pcsCase.getCaseReference());
            notificationService
                .sendDefendantResponseCounterclaimPaymentSuccessEmailNotification(defendantResponse, paymentReference);
        } else if (legalRepresentativeOrganisationEntity != null
            && Objects.equals(userUUID,legalRepresentativeOrganisationEntity.getId())) {
            log.info("Sending counterclaim payment success email to legal representative case reference {}",
                     pcsCase.getCaseReference());
            notificationService.sendDefendantResponseCounterclaimToLegalRepresentativePaymentSuccess(
                legalRepresentativeOrganisationEntity,
                paymentReference,
                defendantResponse.getPcsCase(),
                defendantResponse);
        } else {
            throw new RuntimeException("Current user does not match defendant or legal representative");
        }
    }
}
