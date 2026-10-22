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

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "testing-support.enabled", havingValue = "true")
public class EnforcementBackdateService {

    private static final String SHIFT = "%1$s = %1$s - make_interval(months => :months, days => :days, secs => :secs)";

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
        // The native updates bypass the persistence context, so loaded entities have to be written and dropped.
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
