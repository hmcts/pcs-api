package uk.gov.hmcts.reform.pcs.enforcement.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnName;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnType;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.enforcement.repository.WarrantReturnRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

/**
 * HDPI-8424: whether a further warrant of possession is free. It is free when the claim's most recent final
 * warrant return closed the warrant as one of {@link #FREE_REPEAT_WARRANT_RETURN_NAMES}, and the application
 * is made within {@value #FREE_REPEAT_WARRANT_MONTHS} calendar months of the claim's first warrant of
 * possession being generated. A warrant is generated when a caseworker approves the application, which is what
 * enf_case.approved_at records.
 */
@Component
public class RepeatWarrantFeeWaiver {

    static final int FREE_REPEAT_WARRANT_MONTHS = 12;

    /**
     * Other reasons are "TBC from service" on HDPI-8424 (OPEN-QUESTIONS 8).
     */
    static final Set<WarrantReturnName> FREE_REPEAT_WARRANT_RETURN_NAMES = Collections.unmodifiableSet(
        EnumSet.of(WarrantReturnName.WARRANT_SUSPENDED_BY_ORDER));

    private final EnforcementOrderRepository enforcementOrderRepository;
    private final WarrantReturnRepository warrantReturnRepository;
    private final Clock ukClock;

    public RepeatWarrantFeeWaiver(EnforcementOrderRepository enforcementOrderRepository,
                                  WarrantReturnRepository warrantReturnRepository,
                                  @Qualifier("ukClock") Clock ukClock) {
        this.enforcementOrderRepository = enforcementOrderRepository;
        this.warrantReturnRepository = warrantReturnRepository;
        this.ukClock = ukClock;
    }

    /**
     * Whether an application for {@code enforcementType} made today on the claim owes no fee.
     */
    @Transactional(readOnly = true)
    public boolean isFeeWaived(UUID claimId, SelectEnforcementType enforcementType) {
        if (claimId == null || enforcementType != SelectEnforcementType.WARRANT) {
            return false;
        }
        boolean closedByAWaivingReturn = warrantReturnRepository
            .findByEnforcementOrderClaimIdOrderByRecordedAtDesc(claimId)
            .stream()
            .filter(warrantReturn -> warrantReturn.getReturnType() == WarrantReturnType.FINAL)
            .findFirst()
            .filter(lastFinal -> FREE_REPEAT_WARRANT_RETURN_NAMES.contains(lastFinal.getReturnName()))
            .filter(lastFinal -> lastFinal.getEnforcementOrder().getStatus() == EnforcementStatus.CLOSED)
            .isPresent();
        if (!closedByAWaivingReturn) {
            return false;
        }
        return firstWarrantGeneratedAt(claimId)
            .map(generatedAt -> isWithinFreePeriod(generatedAt, LocalDate.now(ukClock)))
            .orElse(false);
    }

    private Optional<Instant> firstWarrantGeneratedAt(UUID claimId) {
        return enforcementOrderRepository.findByClaimId(claimId).stream()
            .filter(enforcement -> SelectEnforcementType.from(enforcement.getEnforcementOrder())
                == SelectEnforcementType.WARRANT)
            .map(EnforcementOrderEntity::getApprovedAt)
            .filter(Objects::nonNull)
            .min(Instant::compareTo);
    }

    /**
     * Calendar months in UK dates, up to and including the day the period ends: a warrant generated on
     * 31 August 2025 makes a further warrant free up to 31 August 2026, and one generated on 29 February
     * 2024 up to 28 February 2025.
     */
    static boolean isWithinFreePeriod(Instant warrantGeneratedAt, LocalDate applicationDate) {
        LocalDate generated = warrantGeneratedAt.atZone(UK_ZONE_ID).toLocalDate();
        return !applicationDate.isAfter(generated.plusMonths(FREE_REPEAT_WARRANT_MONTHS));
    }
}
