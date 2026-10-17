package uk.gov.hmcts.reform.pcs.enforcement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Finds the enforcement that bailiff staff act on: the claim's live enforcement if it has one, otherwise
 * the one whose status changed most recently, so a closed enforcement can still be worked on.
 */
@Service
@RequiredArgsConstructor
public class CurrentEnforcementService {

    static final Comparator<EnforcementOrderEntity> CURRENT_FIRST =
        Comparator.comparing(CurrentEnforcementService::isLive)
            .thenComparing(EnforcementOrderEntity::getStatusUpdatedAt,
                           Comparator.nullsFirst(Comparator.<Instant>naturalOrder()))
            .reversed();

    private final PcsCaseService pcsCaseService;
    private final EnforcementOrderRepository enforcementOrderRepository;

    public Optional<EnforcementOrderEntity> findCurrentEnforcement(long caseReference) {
        return findCurrentEnforcement(pcsCaseService.loadCase(caseReference));
    }

    public Optional<EnforcementOrderEntity> findCurrentEnforcement(PcsCaseEntity pcsCaseEntity) {
        if (pcsCaseEntity == null) {
            return Optional.empty();
        }
        // One claim per case, as elsewhere in enforcement.
        List<ClaimEntity> claims = pcsCaseEntity.getClaims();
        if (claims == null || claims.isEmpty()) {
            return Optional.empty();
        }
        return enforcementOrderRepository.findByClaimId(claims.getFirst().getId()).stream()
            .min(CURRENT_FIRST);
    }

    private static boolean isLive(EnforcementOrderEntity enforcement) {
        return enforcement.getStatus() != null && enforcement.getStatus().isLive();
    }
}
