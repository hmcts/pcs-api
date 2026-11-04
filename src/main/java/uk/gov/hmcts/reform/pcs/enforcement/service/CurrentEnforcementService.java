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
import java.util.UUID;

/**
 * The current enforcement is the claim's live one, or else the one that changed status most recently.
 */
@Service
@RequiredArgsConstructor
public class CurrentEnforcementService {

    static final Comparator<EnforcementOrderEntity> CURRENT_FIRST =
        Comparator.comparing((EnforcementOrderEntity enforcement) -> enforcement.getStatus().isLive())
            .thenComparing(EnforcementOrderEntity::getStatusUpdatedAt,
                           Comparator.nullsFirst(Comparator.<Instant>naturalOrder()))
            .reversed();

    private final PcsCaseService pcsCaseService;
    private final EnforcementOrderRepository enforcementOrderRepository;

    /**
     * Caches each claim's enforcements for the duration of {@code action}, so a change made inside is not seen.
     */
    public static void withEnforcementsLoadedOnce(Runnable action) {
        EnforcementCaseViewLoads.with(action);
    }

    public Optional<EnforcementOrderEntity> findCurrentEnforcement(long caseReference) {
        return findCurrentEnforcement(pcsCaseService.loadCase(caseReference));
    }

    public Optional<EnforcementOrderEntity> findCurrentEnforcement(PcsCaseEntity pcsCaseEntity) {
        return findEnforcementsCurrentFirst(pcsCaseEntity).stream().findFirst();
    }

    public List<EnforcementOrderEntity> findEnforcementsCurrentFirst(PcsCaseEntity pcsCaseEntity) {
        if (pcsCaseEntity == null) {
            return List.of();
        }
        List<ClaimEntity> claims = pcsCaseEntity.getClaims();
        if (claims == null || claims.isEmpty()) {
            return List.of();
        }
        return EnforcementCaseViewLoads.enforcements(claims.getFirst().getId(), this::loadCurrentFirst);
    }

    private List<EnforcementOrderEntity> loadCurrentFirst(UUID claimId) {
        return enforcementOrderRepository.findByClaimId(claimId).stream()
            .filter(enforcement -> enforcement.getStatus() != null)
            .sorted(CURRENT_FIRST)
            .toList();
    }
}
