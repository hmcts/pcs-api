package uk.gov.hmcts.reform.pcs.enforcement.service;

import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * What defendants, and their legal representatives, may see of a claim's enforcements (HDPI-7539, HDPI-7658,
 * HDPI-8442), so that enforcement is not put at risk and a without notice eviction stays hidden.
 *
 * <ul>
 *     <li>A warrant is visible once its notice of eviction has been served, which is never when the court has
 *     dispensed with service ({@link EvictionScheduleService#defendantsCanSeeEnforcement}). It stays visible
 *     after it is closed.</li>
 *     <li>A writ never is: it is enforced by a High Court Enforcement Officer and no notice is served
 *     through PCS.</li>
 *     <li>While a warrant or writ of restitution is live, nothing is: restitution is enforced without notice,
 *     so defendants lose sight of earlier warrants too until it stops being live, such as when the bailiff
 *     records its final return.</li>
 * </ul>
 *
 * <p>Of the enforcement documents, defendants only ever see the notice of eviction of a visible warrant: never
 * the warrant or writ itself (HDPI-9566), the risk assessment (HDPI-7350) or the claimant's evidence.</p>
 */
public final class DefendantEnforcementRules {

    static final Set<SelectEnforcementType> WARRANTS = Collections.unmodifiableSet(
        EnumSet.of(SelectEnforcementType.WARRANT, SelectEnforcementType.WARRANT_OF_RESTITUTION));

    static final Set<SelectEnforcementType> RESTITUTION = Collections.unmodifiableSet(
        EnumSet.of(SelectEnforcementType.WARRANT_OF_RESTITUTION, SelectEnforcementType.WRIT_OF_RESTITUTION));

    private DefendantEnforcementRules() {
    }

    /**
     * The enforcements defendants may see, in the order given.
     */
    public static List<EnforcementOrderEntity> visibleToDefendants(Collection<EnforcementOrderEntity> enforcements) {
        if (enforcements == null || enforcements.stream().anyMatch(DefendantEnforcementRules::isLiveRestitution)) {
            return List.of();
        }
        return enforcements.stream()
            .filter(DefendantEnforcementRules::isWarrant)
            .filter(EnforcementOrderEntity::isDefendantsCanSeeEnforcement)
            .toList();
    }

    /**
     * The ids of the enforcement documents defendants may see: the notices of eviction of the visible warrants.
     */
    public static Set<UUID> documentIdsVisibleToDefendants(Collection<EnforcementOrderEntity> enforcements) {
        return visibleToDefendants(enforcements).stream()
            .map(EnforcementOrderEntity::getDocuments)
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .filter(document -> document.getType() == DocumentType.NOTICE_OF_EVICTION)
            .map(DocumentEntity::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toUnmodifiableSet());
    }

    private static boolean isWarrant(EnforcementOrderEntity enforcement) {
        return BailiffEnforcementRules.isWarrant(enforcement)
            || WARRANTS.contains(SelectEnforcementType.from(enforcement.getEnforcementOrder()));
    }

    private static boolean isLiveRestitution(EnforcementOrderEntity enforcement) {
        boolean restitution = enforcement.getWarrantOfRestitutionDetails() != null
            || RESTITUTION.contains(SelectEnforcementType.from(enforcement.getEnforcementOrder()));
        return restitution && enforcement.getStatus() != null && enforcement.getStatus().isLive();
    }
}
