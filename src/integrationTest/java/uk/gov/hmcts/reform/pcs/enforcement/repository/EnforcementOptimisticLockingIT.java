package uk.gov.hmcts.reform.pcs.enforcement.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.StatementOfTruthRepository;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementDecisionService;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The enforcement is updated from the case event, the Payments callback and the document generation task,
 * which take no common lock, so enf_case carries a version column (V057) and a write from a stale copy is
 * refused rather than overwriting someone else's.
 */
@SpringBootTest(properties = "db-scheduler.polling-interval-seconds=3600")
@ActiveProfiles("integration")
@DisplayName("Enforcement optimistic locking (V057)")
class EnforcementOptimisticLockingIT extends AbstractPostgresContainerIT {

    @Autowired
    private EnforcementOrderRepository enforcementOrderRepository;

    @Autowired
    private EnforcementDecisionService enforcementDecisionService;

    @Autowired
    private StatementOfTruthRepository statementOfTruthRepository;

    @Autowired
    private PcsCaseRepository pcsCaseRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("bumps the version on every change, and not on a read")
    void bumpsTheVersionOnEveryChange() {
        UUID id = savedEnforcement(5201520152015201L, EnforcementStatus.AWAITING_SCHEDULING);
        // Not 0: a newly built enforcement order does not equal its JSON round trip, so Hibernate updates the
        // row once more in the flush that inserts it.
        final int afterInsert = version(id);

        transactionTemplate.executeWithoutResult(status -> enforcementOrderRepository.findById(id).orElseThrow());

        assertThat(version(id)).isEqualTo(afterInsert);

        transactionTemplate.executeWithoutResult(status ->
            enforcementOrderRepository.findById(id).orElseThrow().setRejectionReason("The order was set aside"));

        assertThat(version(id)).isEqualTo(afterInsert + 1);
    }

    @Test
    @DisplayName("refuses a write from a copy loaded before another write committed")
    void refusesAStaleWrite() {
        UUID id = savedEnforcement(5202520252025202L, EnforcementStatus.AWAITING_SCHEDULING);
        EnforcementOrderEntity stale =
            transactionTemplate.execute(status -> enforcementOrderRepository.findById(id).orElseThrow());

        transactionTemplate.executeWithoutResult(status ->
            enforcementOrderRepository.findById(id).orElseThrow().setStatus(EnforcementStatus.CLOSED));
        stale.setEnforcementNumber("WP-00000042");

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(
            status -> enforcementOrderRepository.saveAndFlush(stale)))
            .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        assertThat(jdbcTemplate.queryForObject(
            "SELECT status FROM enf_case WHERE id = ?", String.class, id)).isEqualTo("CLOSED");
        assertThat(jdbcTemplate.queryForObject(
            "SELECT enforcement_number FROM enf_case WHERE id = ?", String.class, id)).isNull();
    }

    @Test
    @Transactional
    @DisplayName("leaves the case event path able to read and write the enforcement")
    void leavesTheEventPathWorking() {
        long caseReference = 5203520352035203L;
        UUID id = savedEnforcementInThisTransaction(caseReference, EnforcementStatus.AWAITING_REVIEW);
        final int versionAtInsert = enforcementOrderRepository.findById(id).orElseThrow().getVersion();
        entityManager.clear();

        enforcementDecisionService.accept(caseReference);
        entityManager.flush();
        entityManager.clear();

        EnforcementOrderEntity reloaded = enforcementOrderRepository.findById(id).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(EnforcementStatus.AWAITING_BAILIFF_ALLOCATION);
        assertThat(reloaded.getEnforcementNumber()).isNotNull();
        assertThat(reloaded.getVersion()).isGreaterThan(versionAtInsert);
    }

    private int version(UUID id) {
        return jdbcTemplate.queryForObject("SELECT version FROM enf_case WHERE id = ?", Integer.class, id);
    }

    private UUID savedEnforcement(long caseReference, EnforcementStatus status) {
        return transactionTemplate.execute(tx -> savedEnforcementInThisTransaction(caseReference, status));
    }

    private UUID savedEnforcementInThisTransaction(long caseReference, EnforcementStatus status) {
        StatementOfTruthEntity statementOfTruth = new StatementOfTruthEntity();
        statementOfTruth.setAccepted(YesOrNo.YES);
        statementOfTruth.setFullName("Test Name");

        EnforcementOrderEntity enforcement = new EnforcementOrderEntity();
        enforcement.setClaim(savedClaim(caseReference));
        enforcement.setEnforcementOrder(EnforcementOrder.builder()
                                            .chooseEnforcementType(DynamicStringList.builder()
                                                .value(DynamicStringListElement.builder()
                                                           .code(SelectEnforcementType.WARRANT.name())
                                                           .label(SelectEnforcementType.WARRANT.getLabel())
                                                           .build())
                                                .build())
                                            .build());
        enforcement.setStatementOfTruth(statementOfTruthRepository.save(statementOfTruth));
        enforcement.setStatus(status);
        enforcement.setStatusUpdatedAt(Instant.now());
        UUID id = enforcementOrderRepository.save(enforcement).getId();
        entityManager.flush();
        entityManager.clear();
        return id;
    }

    private ClaimEntity savedClaim(long caseReference) {
        PcsCaseEntity pcsCase = PcsCaseEntity.builder().caseReference(caseReference).build();
        ClaimEntity claim = new ClaimEntity();
        pcsCase.addClaim(claim);
        return pcsCaseRepository.save(pcsCase).getClaims().getFirst();
    }
}
