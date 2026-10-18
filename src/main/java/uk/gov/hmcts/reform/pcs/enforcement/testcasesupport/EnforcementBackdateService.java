package uk.gov.hmcts.reform.pcs.enforcement.testcasesupport;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.service.CurrentEnforcementService;

import java.time.Duration;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Moves one enforcement's timestamps into the past, so that the rules measured from them (the claimant's 72
 * hours to confirm an eviction, the free repeat warrant within 12 months of the first being generated) can be
 * exercised without waiting.
 *
 * <p>Every timestamp the enforcement owns moves by the same amount, so their order relative to each other is
 * kept. Several of the columns are {@code updatable = false} in the entities, which is why this is plain SQL.
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "testing-support.enabled", havingValue = "true")
public class EnforcementBackdateService {

    private static final String SHIFT = "%1$s = %1$s - make_interval(months => :months, days => :days, secs => :secs)";

    /**
     * Each table holding the enforcement's timestamps, its timestamp columns, and the column naming the
     * enforcement.
     */
    static final List<TimestampColumns> TIMESTAMP_COLUMNS = List.of(
        new TimestampColumns("enf_case", "id",
                             List.of("bailiff_date", "status_updated_at", "approved_at", "risk_level_set_at",
                                     "notice_served_at")),
        new TimestampColumns("enf_warrant", "enf_case_id", List.of("created")),
        new TimestampColumns("enf_writ", "enf_case_id", List.of("created")),
        new TimestampColumns("enf_warrant_of_restitution", "enf_case_id", List.of("created")),
        new TimestampColumns("enf_writ_of_restitution", "enf_case_id", List.of("created")),
        new TimestampColumns("enf_bailiff_allocation", "enf_case_id", List.of("started_at", "ended_at")),
        new TimestampColumns("enf_bailiff_note", "enf_case_id", List.of("created_at")),
        new TimestampColumns("enf_warrant_return", "enf_case_id", List.of("eviction_at", "recorded_at")),
        new TimestampColumns("document", "enf_case_id", List.of("submitted_date"))
    );

    private final PcsCaseService pcsCaseService;
    private final CurrentEnforcementService currentEnforcementService;
    private final EntityManager entityManager;

    /**
     * Moves the timestamps of the enforcement numbered {@code enforcementNumber} on the case, or of the case's
     * current enforcement when no number is given, back by {@code period} plus {@code duration}.
     *
     * @return the id of the enforcement that was moved, or empty when the case has no such enforcement
     */
    @Transactional
    public Optional<UUID> backdate(long caseReference, String enforcementNumber, Period period, Duration duration) {
        Optional<UUID> enforcementId = findEnforcement(caseReference, enforcementNumber)
            .map(EnforcementOrderEntity::getId);
        enforcementId.ifPresent(id -> shift(id, period == null ? Period.ZERO : period,
                                            duration == null ? Duration.ZERO : duration));
        return enforcementId;
    }

    private Optional<EnforcementOrderEntity> findEnforcement(long caseReference, String enforcementNumber) {
        List<EnforcementOrderEntity> enforcements =
            currentEnforcementService.findEnforcementsCurrentFirst(pcsCaseService.loadCase(caseReference));
        if (enforcementNumber == null || enforcementNumber.isBlank()) {
            return enforcements.stream().findFirst();
        }
        return enforcements.stream()
            .filter(enforcement -> enforcementNumber.equals(enforcement.getEnforcementNumber()))
            .findFirst();
    }

    private void shift(UUID enforcementId, Period period, Duration duration) {
        // Write anything pending, then drop the loaded entities before shifting. Hibernate treats the
        // enforcement_order JSON column as dirty on every flush, and each native update auto-flushes first, so
        // an enf_case entity left in the persistence context is written back over the shifted values.
        entityManager.flush();
        entityManager.clear();
        for (TimestampColumns table : TIMESTAMP_COLUMNS) {
            entityManager.createNativeQuery(table.updateSql())
                .setParameter("months", (int) period.toTotalMonths())
                .setParameter("days", period.getDays())
                .setParameter("secs", duration.toMillis() / 1000.0)
                .setParameter("id", enforcementId)
                .executeUpdate();
        }
    }

    record TimestampColumns(String table, String enforcementIdColumn, List<String> columns) {

        String updateSql() {
            List<String> shifts = columns.stream().map(SHIFT::formatted).toList();
            return "UPDATE " + table + " SET " + String.join(", ", shifts)
                + " WHERE " + enforcementIdColumn + " = :id";
        }
    }
}
