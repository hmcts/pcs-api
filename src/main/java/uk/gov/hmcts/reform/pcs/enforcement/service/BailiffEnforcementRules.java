package uk.gov.hmcts.reform.pcs.enforcement.service;

import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_PAYMENT;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_REVIEW;

/**
 * Which enforcements bailiffs work on. A warrant of restitution counts as a warrant; a writ goes to a High
 * Court Enforcement Officer instead.
 */
public final class BailiffEnforcementRules {

    public static final Set<EnforcementStatus> APPROVED_LIVE_STATUSES = Collections.unmodifiableSet(
        Arrays.stream(EnforcementStatus.values())
            .filter(EnforcementStatus::isLive)
            .filter(status -> status != AWAITING_PAYMENT && status != AWAITING_REVIEW)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(EnforcementStatus.class))));

    private BailiffEnforcementRules() {
    }

    public static boolean isWarrant(EnforcementOrderEntity enforcement) {
        return enforcement.getWarrantDetails() != null || enforcement.getWarrantOfRestitutionDetails() != null;
    }

    public static boolean isApprovedLiveWarrant(EnforcementOrderEntity enforcement) {
        return isWarrant(enforcement) && APPROVED_LIVE_STATUSES.contains(enforcement.getStatus());
    }

    public static boolean isGeneratedWarrant(EnforcementOrderEntity enforcement) {
        return isWarrant(enforcement) && enforcement.getStatus() != null && enforcement.getStatus().isGenerated();
    }
}
