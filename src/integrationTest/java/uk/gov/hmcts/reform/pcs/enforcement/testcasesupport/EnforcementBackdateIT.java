package uk.gov.hmcts.reform.pcs.enforcement.testcasesupport;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.StatementOfTruthRepository;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnName;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnOutcome;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnType;
import uk.gov.hmcts.reform.pcs.enforcement.entity.BailiffNoteEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WarrantReturnEntity;
import uk.gov.hmcts.reform.pcs.enforcement.repository.BailiffNoteRepository;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.enforcement.repository.WarrantReturnRepository;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
@DisplayName("Testing support: backdating an enforcement")
class EnforcementBackdateIT extends AbstractPostgresContainerIT {

    private static final long CASE_REFERENCE = 5555666677778888L;
    private static final LocalDateTime EVICTION = LocalDateTime.parse("2026-07-08T10:00:00");
    private static final Instant APPROVED = Instant.parse("2026-01-31T09:00:00Z");
    private static final Instant NOTE_ADDED = Instant.parse("2026-07-01T12:00:00Z");
    private static final Instant RETURNED = Instant.parse("2026-07-08T15:00:00Z");

    @Autowired
    private EnforcementBackdateService enforcementBackdateService;
    @Autowired
    private EnforcementOrderRepository enforcementOrderRepository;
    @Autowired
    private BailiffNoteRepository bailiffNoteRepository;
    @Autowired
    private WarrantReturnRepository warrantReturnRepository;
    @Autowired
    private StatementOfTruthRepository statementOfTruthRepository;
    @Autowired
    private PcsCaseRepository pcsCaseRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("moves the eviction, approval, note and return times back by the same period")
    void movesEveryTimestampBackByAPeriod() {
        UUID id = savedWarrant("WP-00000101");

        assertThat(enforcementBackdateService.backdate(CASE_REFERENCE, null, Period.ofMonths(13), null))
            .contains(id);

        assertThat(enfCaseTimestamp("bailiff_date", id)).isEqualTo(Timestamp.valueOf(EVICTION.minusMonths(13)));
        // Calendar months in the database's time zone: 31 January less 13 months is 31 December.
        assertThat(enfCaseTimestamp("approved_at", id).toInstant())
            .isEqualTo(Instant.parse("2024-12-31T09:00:00Z"));
        assertThat(childTimestamp("enf_bailiff_note", "created_at", id).toInstant())
            .isEqualTo(Instant.parse("2025-06-01T12:00:00Z"));
        assertThat(childTimestamp("enf_warrant_return", "recorded_at", id).toInstant())
            .isEqualTo(Instant.parse("2025-06-08T15:00:00Z"));
        assertThat(childTimestamp("enf_warrant_return", "eviction_at", id).toInstant())
            .isEqualTo(Instant.parse("2025-06-08T15:00:00Z"));
    }

    @Test
    @DisplayName("moves the eviction back by a number of hours")
    void movesTheEvictionBackByHours() {
        UUID id = savedWarrant("WP-00000102");

        assertThat(enforcementBackdateService.backdate(CASE_REFERENCE, null, null, Duration.ofHours(73)))
            .contains(id);

        assertThat(enfCaseTimestamp("bailiff_date", id)).isEqualTo(Timestamp.valueOf(EVICTION.minusHours(73)));
    }

    @Test
    @DisplayName("moves only the enforcement it is given the number of")
    void movesOnlyTheNumberedEnforcement() {
        UUID first = savedWarrant("WP-00000103");
        UUID second = savedWarrant("WP-00000104");

        assertThat(enforcementBackdateService.backdate(CASE_REFERENCE, "WP-00000103", Period.ofDays(1), null))
            .contains(first);

        assertThat(enfCaseTimestamp("bailiff_date", first)).isEqualTo(Timestamp.valueOf(EVICTION.minusDays(1)));
        assertThat(enfCaseTimestamp("bailiff_date", second)).isEqualTo(Timestamp.valueOf(EVICTION));
    }

    @Test
    @DisplayName("changes nothing when the case has no enforcement with that number")
    void changesNothingForAnUnknownNumber() {
        UUID id = savedWarrant("WP-00000105");

        assertThat(enforcementBackdateService.backdate(CASE_REFERENCE, "WP-99999999", Period.ofDays(1), null))
            .isEmpty();

        assertThat(enfCaseTimestamp("bailiff_date", id)).isEqualTo(Timestamp.valueOf(EVICTION));
    }

    private Timestamp enfCaseTimestamp(String column, UUID id) {
        return jdbcTemplate.queryForObject("SELECT " + column + " FROM enf_case WHERE id = ?", Timestamp.class, id);
    }

    private Timestamp childTimestamp(String table, String column, UUID enforcementId) {
        return jdbcTemplate.queryForObject(
            "SELECT " + column + " FROM " + table + " WHERE enf_case_id = ?", Timestamp.class, enforcementId);
    }

    private UUID savedWarrant(String enforcementNumber) {
        ClaimEntity claim = pcsCaseRepository.findByCaseReference(CASE_REFERENCE)
            .map(pcsCase -> pcsCase.getClaims().getFirst())
            .orElseGet(this::savedClaim);

        StatementOfTruthEntity statementOfTruth = new StatementOfTruthEntity();
        statementOfTruth.setAccepted(YesOrNo.YES);
        statementOfTruth.setFullName("Test Name");

        EnforcementOrderEntity enforcement = new EnforcementOrderEntity();
        enforcement.setClaim(claim);
        enforcement.setEnforcementOrder(EnforcementOrder.builder().build());
        enforcement.setStatementOfTruth(statementOfTruthRepository.save(statementOfTruth));
        enforcement.setStatus(EnforcementStatus.CLOSED);
        enforcement.setStatusUpdatedAt(RETURNED);
        enforcement.setEnforcementNumber(enforcementNumber);
        enforcement.setApprovedAt(APPROVED);
        enforcement.setBailiffDate(EVICTION);
        EnforcementOrderEntity saved = enforcementOrderRepository.save(enforcement);

        bailiffNoteRepository.save(BailiffNoteEntity.builder()
                                       .enforcementOrder(saved)
                                       .note("Dog at the property")
                                       .createdAt(NOTE_ADDED)
                                       .build());
        warrantReturnRepository.save(WarrantReturnEntity.builder()
                                         .enforcementOrder(saved)
                                         .returnType(WarrantReturnType.FINAL)
                                         .outcome(WarrantReturnOutcome.EVICTION_SUCCESSFUL)
                                         .returnName(WarrantReturnName.EVICTION_SUCCESSFUL)
                                         .evictionAt(RETURNED)
                                         .recordedAt(RETURNED)
                                         .recordedBy("bailiff-uid")
                                         .build());
        entityManager.flush();
        return saved.getId();
    }

    private ClaimEntity savedClaim() {
        PcsCaseEntity pcsCase = PcsCaseEntity.builder().caseReference(CASE_REFERENCE).build();
        ClaimEntity claim = new ClaimEntity();
        pcsCase.addClaim(claim);
        return pcsCaseRepository.save(pcsCase).getClaims().getFirst();
    }
}
