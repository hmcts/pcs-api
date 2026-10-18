package uk.gov.hmcts.reform.pcs.enforcement.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import uk.gov.hmcts.reform.pcs.enforcement.domain.RiskLevel;
import uk.gov.hmcts.reform.pcs.enforcement.entity.BailiffAllocationEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WarrantEntity;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffAllocationService;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffUser;
import uk.gov.hmcts.reform.pcs.enforcement.service.CurrentEnforcementService;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementStatusTransitions;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementWaTaskService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
@DisplayName("Bailiff allocation and risk level persistence (V042)")
class BailiffAllocationAndRiskLevelPersistenceIT extends AbstractPostgresContainerIT {

    private static final Instant FIRST_FEB = Instant.parse("2026-02-01T16:20:00Z");
    private static final Instant THIRD_FEB = Instant.parse("2026-02-03T09:00:00Z");

    @Autowired
    private BailiffAllocationRepository bailiffAllocationRepository;

    @Autowired
    private EnforcementOrderRepository enforcementOrderRepository;

    @Autowired
    private WarrantRepository warrantRepository;

    @Autowired
    private StatementOfTruthRepository statementOfTruthRepository;

    @Autowired
    private PcsCaseRepository pcsCaseRepository;

    @Autowired
    private CurrentEnforcementService currentEnforcementService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("stores an allocation with the bailiff's details against the enforcement")
    void storesAnAllocation() {
        EnforcementOrderEntity enforcement = savedEnforcement(1111222233334444L, EnforcementStatus.AWAITING_SCHEDULING);

        UUID id = saveAllocation(enforcement, "sam-id", FIRST_FEB, null);

        BailiffAllocationEntity reloaded = bailiffAllocationRepository.findById(id).orElseThrow();
        assertThat(reloaded.getBailiffUserId()).isEqualTo("sam-id");
        assertThat(reloaded.getBailiffName()).isEqualTo("Name of sam-id");
        assertThat(reloaded.getBailiffEmail()).isEqualTo("sam-id@justice.gov.uk");
        assertThat(reloaded.getAllocatedByUserId()).isEqualTo("manager-id");
        assertThat(reloaded.getStartedAt()).isEqualTo(FIRST_FEB);
        assertThat(reloaded.getEndedAt()).isNull();
        assertThat(reloaded.isActive()).isTrue();
        assertThat(reloaded.getEnforcementOrder().getId()).isEqualTo(enforcement.getId());
        assertThat(bailiffAllocationRepository.findByEnforcementOrderIdAndEndedAtIsNull(enforcement.getId()))
            .extracting(BailiffAllocationEntity::getId)
            .containsExactly(id);
    }

    @Test
    @DisplayName("allows only one active allocation per enforcement")
    void allowsOneActiveAllocation() {
        EnforcementOrderEntity enforcement = savedEnforcement(2222333344445555L, EnforcementStatus.AWAITING_SCHEDULING);
        saveAllocation(enforcement, "sam-id", FIRST_FEB, null);

        assertThatThrownBy(() -> insertAllocation(enforcement.getId(), "alex-id", null))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("keeps ended allocations alongside the active one, oldest first")
    void keepsTheAllocationHistory() {
        EnforcementOrderEntity enforcement = savedEnforcement(3333444455556666L, EnforcementStatus.AWAITING_SCHEDULING);
        UUID ended = saveAllocation(enforcement, "sam-id", FIRST_FEB, THIRD_FEB);
        UUID active = saveAllocation(enforcement, "alex-id", THIRD_FEB, null);

        assertThat(bailiffAllocationRepository.findByEnforcementOrderIdOrderByStartedAtAsc(enforcement.getId()))
            .extracting(BailiffAllocationEntity::getId)
            .containsExactly(ended, active);
        assertThat(bailiffAllocationRepository.findByEnforcementOrderIdAndEndedAtIsNull(enforcement.getId()))
            .extracting(BailiffAllocationEntity::getId)
            .containsExactly(active);
    }

    @Test
    @DisplayName("refuses an allocation that ends before it starts")
    void refusesAnAllocationEndingBeforeItStarts() {
        EnforcementOrderEntity enforcement = savedEnforcement(4444555566667777L, EnforcementStatus.AWAITING_SCHEDULING);

        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO enf_bailiff_allocation (id, enf_case_id, bailiff_user_id, started_at, ended_at) "
                + "VALUES (?, ?, 'sam-id', now(), now() - interval '1 day')",
            UUID.randomUUID(), enforcement.getId()))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("requires the allocation to belong to an existing enforcement")
    void requiresAnExistingEnforcement() {
        assertThatThrownBy(() -> insertAllocation(UUID.randomUUID(), "sam-id", null))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("requires the allocation to name the bailiff")
    void requiresTheBailiff() {
        EnforcementOrderEntity enforcement = savedEnforcement(5555666677778888L, EnforcementStatus.AWAITING_SCHEDULING);

        assertThatThrownBy(() -> insertAllocation(enforcement.getId(), null, null))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("removes the allocations with their enforcement")
    void removesAllocationsWithTheirEnforcement() {
        EnforcementOrderEntity enforcement = savedEnforcement(6666777788889999L, EnforcementStatus.AWAITING_SCHEDULING);
        UUID id = saveAllocation(enforcement, "sam-id", FIRST_FEB, null);

        jdbcTemplate.update("DELETE FROM enf_case WHERE id = ?", enforcement.getId());

        assertThat(jdbcTemplate.queryForObject(
            "SELECT count(*) FROM enf_bailiff_allocation WHERE id = ?", Integer.class, id)).isZero();
    }

    @Test
    @DisplayName("allocates, then reallocates, leaving only the new bailiff active")
    void allocatesAndReallocatesThroughTheService() {
        long caseReference = 7777888899990000L;
        final EnforcementOrderEntity enforcement =
            savedEnforcement(caseReference, EnforcementStatus.AWAITING_BAILIFF_ALLOCATION);
        BailiffAllocationService service = allocationService(FIRST_FEB);

        service.allocate(caseReference, new BailiffUser("sam-id", "Sam Jones", "sam@justice.gov.uk"));
        entityManager.flush();
        allocationService(THIRD_FEB)
            .allocate(caseReference, new BailiffUser("alex-id", "Alex Smith", "alex@justice.gov.uk"));
        entityManager.flush();
        entityManager.clear();

        assertThat(bailiffAllocationRepository.findByEnforcementOrderIdOrderByStartedAtAsc(enforcement.getId()))
            .extracting(BailiffAllocationEntity::getBailiffUserId, BailiffAllocationEntity::getEndedAt)
            .containsExactly(
                tuple("sam-id", THIRD_FEB),
                tuple("alex-id", null));
        EnforcementOrderEntity reloaded = enforcementOrderRepository.findById(enforcement.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(EnforcementStatus.AWAITING_SCHEDULING);
        assertThat(reloaded.getStatusUpdatedAt()).isEqualTo(FIRST_FEB);
    }

    @Test
    @DisplayName("stores the risk level, its reason and when it was set on the enforcement")
    void storesTheRiskLevel() {
        EnforcementOrderEntity enforcement = savedEnforcement(1212121212121212L, EnforcementStatus.AWAITING_SCHEDULING);
        enforcement.setRiskLevel(RiskLevel.HIGH);
        enforcement.setRiskLevelReason("a".repeat(6800));
        enforcement.setRiskLevelSetAt(FIRST_FEB);
        enforcementOrderRepository.save(enforcement);
        entityManager.flush();
        entityManager.clear();

        EnforcementOrderEntity reloaded = enforcementOrderRepository.findById(enforcement.getId()).orElseThrow();
        assertThat(reloaded.getRiskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(reloaded.getRiskLevelReason()).hasSize(6800);
        assertThat(reloaded.getRiskLevelSetAt()).isEqualTo(FIRST_FEB);
        assertThat(jdbcTemplate.queryForObject(
            "SELECT risk_level FROM enf_case WHERE id = ?", String.class, enforcement.getId())).isEqualTo("HIGH");
    }

    @Test
    @DisplayName("allows a medium risk level without a reason")
    void allowsMediumWithoutAReason() {
        EnforcementOrderEntity enforcement = savedEnforcement(1313131313131313L, EnforcementStatus.AWAITING_SCHEDULING);

        jdbcTemplate.update("UPDATE enf_case SET risk_level = 'MEDIUM' WHERE id = ?", enforcement.getId());

        assertThat(jdbcTemplate.queryForObject(
            "SELECT risk_level FROM enf_case WHERE id = ?", String.class, enforcement.getId())).isEqualTo("MEDIUM");
    }

    // A refused statement aborts the test's transaction, so each of these checks one constraint.

    @Test
    @DisplayName("refuses a high risk level without a reason")
    void refusesHighWithoutAReason() {
        UUID id = savedEnforcement(1414141414141414L, EnforcementStatus.AWAITING_SCHEDULING).getId();

        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE enf_case SET risk_level = 'HIGH' WHERE id = ?", id))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("refuses an unknown risk level")
    void refusesAnUnknownRiskLevel() {
        UUID id = savedEnforcement(1616161616161616L, EnforcementStatus.AWAITING_SCHEDULING).getId();

        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE enf_case SET risk_level = 'LOW' WHERE id = ?", id))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("refuses a reason longer than 6,800 characters")
    void refusesAnOverLongReason() {
        UUID id = savedEnforcement(1717171717171717L, EnforcementStatus.AWAITING_SCHEDULING).getId();

        assertThatThrownBy(() -> jdbcTemplate.update(
            "UPDATE enf_case SET risk_level = 'HIGH', risk_level_reason = ? WHERE id = ?", "a".repeat(6801), id))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("leaves existing enforcements with no risk level")
    void leavesExistingEnforcementsWithNoRiskLevel() {
        EnforcementOrderEntity enforcement = savedEnforcement(1515151515151515L, EnforcementStatus.AWAITING_REVIEW);
        entityManager.clear();

        EnforcementOrderEntity reloaded = enforcementOrderRepository.findById(enforcement.getId()).orElseThrow();
        assertThat(reloaded.getRiskLevel()).isNull();
        assertThat(reloaded.getRiskLevelReason()).isNull();
        assertThat(reloaded.getRiskLevelSetAt()).isNull();
    }

    private BailiffAllocationService allocationService(Instant now) {
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        SecurityContextService securityContextService = mock(SecurityContextService.class);
        when(securityContextService.getCurrentUserId()).thenReturn("manager-id");
        return new BailiffAllocationService(currentEnforcementService, new EnforcementStatusTransitions(clock),
                                            bailiffAllocationRepository, enforcementOrderRepository,
                                            securityContextService, mock(EnforcementWaTaskService.class), clock);
    }

    private void insertAllocation(UUID enforcementId, String bailiffUserId, Instant endedAt) {
        jdbcTemplate.update(
            "INSERT INTO enf_bailiff_allocation (id, enf_case_id, bailiff_user_id, started_at, ended_at) "
                + "VALUES (?, ?, ?, now(), ?)",
            UUID.randomUUID(), enforcementId, bailiffUserId,
            endedAt == null ? null : Timestamp.from(endedAt));
    }

    private UUID saveAllocation(EnforcementOrderEntity enforcement, String bailiffUserId, Instant startedAt,
                                Instant endedAt) {
        UUID id = bailiffAllocationRepository.save(BailiffAllocationEntity.builder()
                                                       .enforcementOrder(enforcement)
                                                       .bailiffUserId(bailiffUserId)
                                                       .bailiffName("Name of " + bailiffUserId)
                                                       .bailiffEmail(bailiffUserId + "@justice.gov.uk")
                                                       .allocatedByUserId("manager-id")
                                                       .startedAt(startedAt)
                                                       .endedAt(endedAt)
                                                       .build()).getId();
        entityManager.flush();
        entityManager.clear();
        return id;
    }

    private EnforcementOrderEntity savedEnforcement(long caseReference, EnforcementStatus status) {
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
        enforcement.setStatus(status);
        enforcement.setStatusUpdatedAt(FIRST_FEB.minus(1, ChronoUnit.DAYS));
        EnforcementOrderEntity saved = enforcementOrderRepository.save(enforcement);
        warrantRepository.save(WarrantEntity.builder().enforcementOrder(saved).build());
        entityManager.flush();
        entityManager.clear();
        return enforcementOrderRepository.findById(saved.getId()).orElseThrow();
    }

    private ClaimEntity savedClaim(long caseReference) {
        PcsCaseEntity pcsCase = PcsCaseEntity.builder().caseReference(caseReference).build();
        ClaimEntity claim = new ClaimEntity();
        pcsCase.addClaim(claim);
        return pcsCaseRepository.save(pcsCase).getClaims().getFirst();
    }
}
