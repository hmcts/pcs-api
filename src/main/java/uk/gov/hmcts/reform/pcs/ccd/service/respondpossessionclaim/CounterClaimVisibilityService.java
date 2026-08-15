package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class CounterClaimVisibilityService {

    // Issued means paid for; only the raiser may attach documents
    public boolean isCounterClaimVisibleToUser(CounterClaimEntity counterClaim, UUID currentUserId) {
        if (counterClaim == null || currentUserId == null) {
            return false;
        }

        if (counterClaim.getStatus() != CounterClaimState.COUNTER_CLAIM_ISSUED) {
            return false;
        }

        PartyEntity raisedBy = counterClaim.getParty();
        return raisedBy != null && currentUserId.equals(raisedBy.getIdamId());
    }

    // One only, until multiple counterclaims per defendant are supported (HDPI-8372)
    public Optional<CounterClaimEntity> getVisibleCounterClaimForUser(
        Collection<CounterClaimEntity> counterClaims, UUID currentUserId) {

        if (counterClaims == null || counterClaims.isEmpty()) {
            return Optional.empty();
        }

        return counterClaims.stream()
            .filter(Objects::nonNull)
            .filter(counterClaim -> isCounterClaimVisibleToUser(counterClaim, currentUserId))
            .max(Comparator.comparing(
                CounterClaimEntity::getClaimSubmittedDate,
                Comparator.nullsFirst(Comparator.naturalOrder())
            ));
    }
}
