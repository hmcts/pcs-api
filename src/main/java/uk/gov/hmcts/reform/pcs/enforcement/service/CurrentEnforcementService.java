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
 * The one place that decides which of a claim's enforcements is the current one: the live enforcement if
 * there is one (a claim has at most one), otherwise the one whose status changed most recently, so a closed
 * enforcement can still be worked on. The case view's "Enforcement status" tab and the bailiff events all use
 * it, so they always agree on which enforcement they mean.
 */
@Service
@RequiredArgsConstructor
public class CurrentEnforcementService {

    /**
     * The live enforcement first, then the rest by when their status last changed, most recent first. Rows
     * with no status change time come last: they predate the status model.
     */
    static final Comparator<EnforcementOrderEntity> CURRENT_FIRST =
        Comparator.comparing((EnforcementOrderEntity enforcement) -> enforcement.getStatus().isLive())
            .thenComparing(EnforcementOrderEntity::getStatusUpdatedAt,
                           Comparator.nullsFirst(Comparator.<Instant>naturalOrder()))
            .reversed();

    private final PcsCaseService pcsCaseService;
    private final EnforcementOrderRepository enforcementOrderRepository;

    public Optional<EnforcementOrderEntity> findCurrentEnforcement(long caseReference) {
        return findCurrentEnforcement(pcsCaseService.loadCase(caseReference));
    }

    public Optional<EnforcementOrderEntity> findCurrentEnforcement(PcsCaseEntity pcsCaseEntity) {
        return findEnforcementsCurrentFirst(pcsCaseEntity).stream().findFirst();
    }

    /**
     * Every enforcement on the case's claim that has a status, the current one first. Empty when the case has
     * no claim or no enforcement.
     */
    public List<EnforcementOrderEntity> findEnforcementsCurrentFirst(PcsCaseEntity pcsCaseEntity) {
        if (pcsCaseEntity == null) {
            return List.of();
        }
        // One claim per case, as elsewhere in enforcement.
        List<ClaimEntity> claims = pcsCaseEntity.getClaims();
        if (claims == null || claims.isEmpty()) {
            return List.of();
        }
        return enforcementOrderRepository.findByClaimId(claims.getFirst().getId()).stream()
            .filter(enforcement -> enforcement.getStatus() != null)
            .sorted(CURRENT_FIRST)
            .toList();
    }
}
