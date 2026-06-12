package uk.gov.hmcts.reform.pcs.notify.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CounterClaimNotificationService {

    private final CounterClaimRepository counterClaimRepository;
    private final NotificationService notificationService;

    @Transactional
    public void sendClaimantEmailNotificationCounterClaimIssued(UUID counterClaimId) {
        CounterClaimEntity counterClaimEntity = counterClaimRepository.findById(counterClaimId)
            .orElseThrow(() -> new IllegalArgumentException("Counter claim not found: " + counterClaimId));

        DefendantResponseEntity defendantResponse = getAssociatedDefendantResponse(counterClaimEntity);

        notificationService.sendClaimantDefendantHasMadeCounterclaimEmail(defendantResponse.getClaim());
    }

    private DefendantResponseEntity getAssociatedDefendantResponse(CounterClaimEntity counterClaim) {
        UUID partyId = counterClaim.getParty().getId();

        return counterClaim.getPcsCase().getDefendantResponses().stream()
            .filter(defendantResponse -> defendantResponse.getParty().getId().equals(partyId))
            .findFirst()
            .orElseThrow(
                () -> new IllegalArgumentException("Associated defendant response not found for counter claim: "
                                                       + counterClaim.getId()));
    }
}
