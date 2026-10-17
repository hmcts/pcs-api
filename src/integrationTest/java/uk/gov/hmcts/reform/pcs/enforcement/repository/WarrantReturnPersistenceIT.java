package uk.gov.hmcts.reform.pcs.enforcement.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
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
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WarrantReturnEntity;
import uk.gov.hmcts.reform.pcs.enforcement.service.WarrantReturnService;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
@DisplayName("Warrant return persistence (V044)")
class WarrantReturnPersistenceIT extends AbstractPostgresContainerIT {

    private static final Instant EVICTED = Instant.parse("2026-07-08T08:15:00Z");
    private static final Instant FIRST = Instant.parse("2026-07-08T09:00:00Z");
    private static final Instant SECOND = Instant.parse("2026-07-09T09:00:00Z");
    private static final Instant THIRD = Instant.parse("2026-07-10T09:00:00Z");

    @Autowired
    private WarrantReturnRepository warrantReturnRepository;

    @Autowired
    private EnforcementOrderRepository enforcementOrderRepository;

    @Autowired
    private StatementOfTruthRepository statementOfTruthRepository;

    @Autowired
    private PcsCaseRepository pcsCaseRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private WarrantReturnService warrantReturnService;

    @Test
    @DisplayName("stores a successful eviction with its outcome, time, details and who recorded it")
    void storesASuccessfulEviction() {
        EnforcementOrderEntity warrant = savedWarrant(1111222233334444L);

        UUID id = save(WarrantReturnEntity.builder()
                           .enforcementOrder(warrant)
                           .returnType(WarrantReturnType.FINAL)
                           .outcome(WarrantReturnOutcome.EVICTION_SUCCESSFUL)
                           .returnName(WarrantReturnName.EVICTION_SUCCESSFUL)
                           .evictionAt(EVICTED)
                           .additionalDetails("Keys handed over")
                           .recordedAt(FIRST)
                           .recordedBy("bailiff-uid"));

        WarrantReturnEntity reloaded = warrantReturnRepository.findById(id).orElseThrow();
        assertThat(reloaded.getEnforcementOrder().getId()).isEqualTo(warrant.getId());
        assertThat(reloaded.getReturnType()).isEqualTo(WarrantReturnType.FINAL);
        assertThat(reloaded.getOutcome()).isEqualTo(WarrantReturnOutcome.EVICTION_SUCCESSFUL);
        assertThat(reloaded.getReturnName()).isEqualTo(WarrantReturnName.EVICTION_SUCCESSFUL);
        assertThat(reloaded.getEvictionAt()).isEqualTo(EVICTED);
        assertThat(reloaded.getAdditionalDetails()).isEqualTo("Keys handed over");
        assertThat(reloaded.getRecordedAt()).isEqualTo(FIRST);
        assertThat(reloaded.getRecordedBy()).isEqualTo("bailiff-uid");
        assertThat(jdbcTemplate.queryForObject(
            "SELECT return_name FROM enf_warrant_return WHERE id = ?", String.class, id))
            .isEqualTo("EVICTION_SUCCESSFUL");
    }

    @Test
    @DisplayName("stores every return name")
    void storesEveryReturnName() {
        EnforcementOrderEntity warrant = savedWarrant(2222333344445555L);

        for (WarrantReturnName returnName : WarrantReturnName.values()) {
            save(interim(warrant, returnName, FIRST));
        }

        assertThat(warrantReturnRepository.findByEnforcementOrderIdOrderByRecordedAtDesc(warrant.getId()))
            .extracting(WarrantReturnEntity::getReturnName)
            .containsExactlyInAnyOrder(WarrantReturnName.values());
    }

    @Test
    @DisplayName("keeps every return of an enforcement as history, newest first")
    void keepsTheReturnHistory() {
        EnforcementOrderEntity warrant = savedWarrant(3333444455556666L);
        EnforcementOrderEntity other = savedWarrant(3333444455556666L);
        UUID first = save(interim(warrant, WarrantReturnName.CROWDS_OR_PROTESTS, FIRST));
        UUID third = save(interim(warrant, WarrantReturnName.REOPEN_WARRANT, THIRD));
        UUID second = save(WarrantReturnEntity.builder()
                               .enforcementOrder(warrant)
                               .returnType(WarrantReturnType.FINAL)
                               .outcome(WarrantReturnOutcome.EVICTION_CANCELLED)
                               .returnName(WarrantReturnName.WARRANT_SUSPENDED_BY_ORDER)
                               .recordedAt(SECOND));
        UUID otherReturn = save(interim(other, WarrantReturnName.OBSTRUCTION_BY_DEFENDANTS, THIRD.plusSeconds(1)));

        assertThat(warrantReturnRepository.findByEnforcementOrderIdOrderByRecordedAtDesc(warrant.getId()))
            .extracting(WarrantReturnEntity::getId)
            .containsExactly(third, second, first);
        assertThat(warrantReturnRepository.findFirstByEnforcementOrderIdOrderByRecordedAtDesc(warrant.getId()))
            .map(WarrantReturnEntity::getId)
            .contains(third);
        assertThat(warrantReturnRepository.findByEnforcementOrderClaimIdOrderByRecordedAtDesc(
            warrant.getClaim().getId()))
            .extracting(WarrantReturnEntity::getId)
            .containsExactly(otherReturn, third, second, first);
    }

    @Test
    @DisplayName("accepts additional details of 6,800 characters and rejects longer ones")
    void enforcesTheAdditionalDetailsLength() {
        EnforcementOrderEntity warrant = savedWarrant(4444555566667777L);

        assertThat(jdbcTemplate.update(insertSql(), UUID.randomUUID(), warrant.getId(), "INTERIM", null,
                                       "OTHER_INTERIM_RETURN", null, "a".repeat(6800))).isEqualTo(1);
        assertThatThrownBy(() -> jdbcTemplate.update(insertSql(), UUID.randomUUID(), warrant.getId(), "INTERIM",
                                                      null, "OTHER_INTERIM_RETURN", null, "a".repeat(6801)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    // Each case is its own test: a failed statement aborts the test's transaction.
    @ParameterizedTest(name = "{3}")
    @CsvSource(nullValues = "null", value = {
        "FINAL, null, WARRANT_EXPIRED, a final return without an outcome",
        "INTERIM, EVICTION_UNSUCCESSFUL, CROWDS_OR_PROTESTS, an interim return with an outcome",
        "PARTIAL, null, CROWDS_OR_PROTESTS, an unknown return type",
        "INTERIM, null, SOMETHING_ELSE, an unknown return name",
        "FINAL, EVICTION_POSTPONED, WARRANT_EXPIRED, an unknown outcome",
        "null, null, CROWDS_OR_PROTESTS, no return type",
        "INTERIM, null, null, no return name"
    })
    @DisplayName("rejects")
    void rejectsAnInvalidReturn(String returnType, String outcome, String returnName, String description) {
        EnforcementOrderEntity warrant = savedWarrant(5555666677778888L);

        assertThatThrownBy(() -> insert(warrant.getId(), returnType, outcome, returnName, null))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("only lets a successful eviction have an eviction time")
    void allowsAnEvictionTimeOnSuccessfulEvictionsOnly() {
        EnforcementOrderEntity warrant = savedWarrant(6666777788889999L);

        assertThatThrownBy(() -> insert(warrant.getId(), "FINAL", "EVICTION_UNSUCCESSFUL", "WARRANT_EXPIRED",
                                        Timestamp.from(EVICTED)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("requires the return to belong to an existing enforcement")
    void requiresAnExistingEnforcement() {
        assertThatThrownBy(() -> insert(UUID.randomUUID(), "INTERIM", null, "CROWDS_OR_PROTESTS", null))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("requires the return to belong to an enforcement")
    void requiresAnEnforcement() {
        assertThatThrownBy(() -> insert(null, "INTERIM", null, "CROWDS_OR_PROTESTS", null))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("removes the returns with their enforcement")
    void removesReturnsWithTheirEnforcement() {
        EnforcementOrderEntity warrant = savedWarrant(8888999900001111L);
        UUID id = save(interim(warrant, WarrantReturnName.CROWDS_OR_PROTESTS, FIRST));

        jdbcTemplate.update("DELETE FROM enf_case WHERE id = ?", warrant.getId());

        assertThat(jdbcTemplate.queryForObject(
            "SELECT count(*) FROM enf_warrant_return WHERE id = ?", Integer.class, id)).isZero();
    }

    @ParameterizedTest(name = "approved {0} months ago: free {1}")
    @CsvSource({"6, true", "13, false"})
    @DisplayName("measures the free further warrant from when the warrant was approved")
    void measuresTheFreeWarrantPeriodFromApproval(int monthsSinceApproval, boolean free) {
        long caseReference = 2222111144443333L + monthsSinceApproval;
        EnforcementOrderEntity warrant = savedWarrant(caseReference);
        warrant.setApprovedAt(LocalDate.now().minusMonths(monthsSinceApproval).atStartOfDay(UK_ZONE_ID).toInstant());
        enforcementOrderRepository.saveAndFlush(warrant);
        // Recorded today, so a period measured from the return would always be free.
        save(WarrantReturnEntity.builder()
            .enforcementOrder(warrant)
            .returnType(WarrantReturnType.FINAL)
            .outcome(WarrantReturnOutcome.EVICTION_UNSUCCESSFUL)
            .returnName(WarrantReturnName.WARRANT_SUSPENDED_BY_ORDER)
            .recordedAt(Instant.now()));

        PcsCaseEntity pcsCase = pcsCaseRepository.findByCaseReference(caseReference).orElseThrow();

        assertThat(warrantReturnService.isNextWarrantFree(pcsCase)).isEqualTo(free);
    }

    private static String insertSql() {
        return "INSERT INTO enf_warrant_return (id, enf_case_id, return_type, outcome, return_name, eviction_at, "
            + "additional_details, recorded_at) VALUES (?, ?, ?, ?, ?, ?, ?, now())";
    }

    private void insert(UUID enforcementId, String returnType, String outcome, String returnName,
                        Timestamp evictionAt) {
        jdbcTemplate.update(insertSql(), UUID.randomUUID(), enforcementId, returnType, outcome, returnName,
                            evictionAt, null);
    }

    private static WarrantReturnEntity.WarrantReturnEntityBuilder interim(EnforcementOrderEntity warrant,
                                                                          WarrantReturnName returnName,
                                                                          Instant recordedAt) {
        return WarrantReturnEntity.builder()
            .enforcementOrder(warrant)
            .returnType(WarrantReturnType.INTERIM)
            .returnName(returnName)
            .recordedAt(recordedAt);
    }

    private UUID save(WarrantReturnEntity.WarrantReturnEntityBuilder warrantReturn) {
        UUID id = warrantReturnRepository.save(warrantReturn.build()).getId();
        entityManager.flush();
        entityManager.clear();
        return id;
    }

    private EnforcementOrderEntity savedWarrant(long caseReference) {
        ClaimEntity claim = pcsCaseRepository.findByCaseReference(caseReference)
            .map(pcsCase -> pcsCase.getClaims().getFirst())
            .orElseGet(() -> savedClaim(caseReference));

        StatementOfTruthEntity statementOfTruth = new StatementOfTruthEntity();
        statementOfTruth.setAccepted(YesOrNo.YES);
        statementOfTruth.setFullName("Test Name");

        EnforcementOrderEntity enforcement = new EnforcementOrderEntity();
        enforcement.setClaim(claim);
        enforcement.setEnforcementOrder(EnforcementOrder.builder().build());
        enforcement.setStatementOfTruth(statementOfTruthRepository.save(statementOfTruth));
        enforcement.setStatus(EnforcementStatus.CLAIMANT_CONFIRMED);
        enforcement.setStatusUpdatedAt(Instant.now());
        EnforcementOrderEntity saved = enforcementOrderRepository.save(enforcement);
        entityManager.flush();
        return saved;
    }

    private ClaimEntity savedClaim(long caseReference) {
        PcsCaseEntity pcsCase = PcsCaseEntity.builder().caseReference(caseReference).build();
        ClaimEntity claim = new ClaimEntity();
        pcsCase.addClaim(claim);
        return pcsCaseRepository.save(pcsCase).getClaims().getFirst();
    }
}
