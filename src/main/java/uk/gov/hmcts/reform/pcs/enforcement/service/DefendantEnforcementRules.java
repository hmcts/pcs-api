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
 * What defendants may see of a claim's enforcements: a warrant once its notice of eviction has been served, and
 * never a writ. While a restitution is live nothing is visible, including earlier warrants.
 */
public final class DefendantEnforcementRules {

    static final Set<SelectEnforcementType> WARRANTS = Collections.unmodifiableSet(
        EnumSet.of(SelectEnforcementType.WARRANT, SelectEnforcementType.WARRANT_OF_RESTITUTION));

    static final Set<SelectEnforcementType> RESTITUTION = Collections.unmodifiableSet(
        EnumSet.of(SelectEnforcementType.WARRANT_OF_RESTITUTION, SelectEnforcementType.WRIT_OF_RESTITUTION));

    private DefendantEnforcementRules() {
    }

    public static List<EnforcementOrderEntity> visibleToDefendants(Collection<EnforcementOrderEntity> enforcements) {
        if (enforcements == null || enforcements.stream().anyMatch(DefendantEnforcementRules::isLiveRestitution)) {
            return List.of();
        }
        return enforcements.stream()
            .filter(DefendantEnforcementRules::isWarrant)
            .filter(EnforcementOrderEntity::isDefendantsCanSeeEnforcement)
            .toList();
    }

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
