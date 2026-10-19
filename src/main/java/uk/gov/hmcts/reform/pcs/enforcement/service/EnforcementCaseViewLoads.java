package uk.gov.hmcts.reform.pcs.enforcement.service;

import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WarrantReturnEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * The enforcement rows one case view reads, each loaded at most once however many of the view's parts ask for
 * it. Open for the duration of {@link CurrentEnforcementService#withEnforcementsLoadedOnce} and held on the
 * thread running it, so only for work that reads enforcements and never changes them: a change made while one
 * is open would not be seen by later reads.
 */
final class EnforcementCaseViewLoads {

    private static final ThreadLocal<EnforcementCaseViewLoads> OPEN = new ThreadLocal<>();

    private final Map<UUID, List<EnforcementOrderEntity>> enforcementsByClaim = new HashMap<>();
    private final Map<UUID, Map<UUID, WarrantReturnEntity>> latestReturnsByClaim = new HashMap<>();

    private EnforcementCaseViewLoads() {
    }

    static void with(Runnable action) {
        if (OPEN.get() != null) {
            action.run();
            return;
        }
        OPEN.set(new EnforcementCaseViewLoads());
        try {
            action.run();
        } finally {
            OPEN.remove();
        }
    }

    /**
     * The claim's enforcements, from {@code load} the first time they are asked for.
     */
    static List<EnforcementOrderEntity> enforcements(UUID claimId,
                                                     Function<UUID, List<EnforcementOrderEntity>> load) {
        EnforcementCaseViewLoads open = OPEN.get();
        return open == null ? load.apply(claimId) : open.enforcementsByClaim.computeIfAbsent(claimId, load);
    }

    /**
     * Every enforcement of the claim's most recent warrant return, by enforcement id, from {@code load} the
     * first time they are asked for. Empty when no case view is open, so that work on a single enforcement
     * loads that enforcement's return on its own rather than the whole claim's.
     */
    static Optional<Map<UUID, WarrantReturnEntity>> latestReturns(
        UUID claimId, Function<UUID, Map<UUID, WarrantReturnEntity>> load) {
        EnforcementCaseViewLoads open = OPEN.get();
        if (open == null || claimId == null) {
            return Optional.empty();
        }
        return Optional.of(open.latestReturnsByClaim.computeIfAbsent(claimId, load));
    }
}
