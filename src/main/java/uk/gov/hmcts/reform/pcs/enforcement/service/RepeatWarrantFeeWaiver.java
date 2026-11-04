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

@Component
public class RepeatWarrantFeeWaiver {

    static final int FREE_REPEAT_WARRANT_MONTHS = 12;

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

    static boolean isWithinFreePeriod(Instant warrantGeneratedAt, LocalDate applicationDate) {
        LocalDate generated = warrantGeneratedAt.atZone(UK_ZONE_ID).toLocalDate();
        return !applicationDate.isAfter(generated.plusMonths(FREE_REPEAT_WARRANT_MONTHS));
    }
}
