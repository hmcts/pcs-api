package uk.gov.hmcts.reform.pcs.notify.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;

import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.OrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.OrganisationRepository;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentNotificationService {

    private final NotificationService notificationService;
    private final CounterClaimRepository counterClaimRepository;
    private final OrganisationRepository organisationRepository;

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

        OrganisationEntity legalRepresentativeOrganisationEntity =
            organisationRepository.findByPartyLinkedToOrganisationAndActive(
                defendantResponse.getParty().getId()).orElse(null);

        if (legalRepresentativeOrganisationEntity != null) {
            log.info("Sending counterclaim payment success email to legal representative case reference {}",
                     pcsCase.getCaseReference());
            notificationService.sendDefendantResponseCounterclaimToOrganisationPaymentSuccess(
                legalRepresentativeOrganisationEntity,
                paymentReference,
                defendantResponse.getPcsCase(),
                defendantResponse);
        } else {
            log.info("Sending counterclaim payment success email case reference {}", pcsCase.getCaseReference());
            notificationService
                .sendDefendantResponseCounterclaimPaymentSuccessEmailNotification(defendantResponse, paymentReference);
        }
    }
}
