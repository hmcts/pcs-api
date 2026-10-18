package uk.gov.hmcts.reform.pcs.enforcement.batchprint;

import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
import uk.gov.hmcts.reform.pcs.enforcement.entity.BailiffAllocationEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WarrantEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WarrantOfRestitutionEntity;
import uk.gov.hmcts.reform.pcs.enforcement.repository.BailiffAllocationRepository;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.enforcement.repository.WarrantOfRestitutionRepository;
import uk.gov.hmcts.reform.pcs.enforcement.repository.WarrantRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The bailiff's warrant list loads in one query however many warrants are allocated, rather than loading each
 * allocation's enforcement, claim, case and warrant details one by one.
 */
@SpringBootTest
@ActiveProfiles("integration")
@Transactional
@DisplayName("Listing a bailiff's allocated warrants")
class AllocatedWarrantsQueryIT extends AbstractPostgresContainerIT {

    private static final String BAILIFF = "bob-id";
    private static final Instant MONDAY = Instant.parse("2026-10-12T08:00:00Z");
    private static final LocalDateTime EVICTION = LocalDateTime.of(2026, 10, 15, 10, 0);

    @Autowired
    private BatchPrintPlanner batchPrintPlanner;
    @Autowired
    private PcsCaseRepository pcsCaseRepository;
    @Autowired
    private EnforcementOrderRepository enforcementOrderRepository;
    @Autowired
    private WarrantRepository warrantRepository;
    @Autowired
    private WarrantOfRestitutionRepository warrantOfRestitutionRepository;
    @Autowired
    private StatementOfTruthRepository statementOfTruthRepository;
    @Autowired
    private BailiffAllocationRepository bailiffAllocationRepository;
    @Autowired
    private EntityManager entityManager;

    private Statistics statistics;

    @BeforeEach
    void enableStatistics() {
        statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
    }

    @AfterEach
    void disableStatistics() {
        statistics.setStatisticsEnabled(false);
    }

    @Test
    @DisplayName("lists approved live warrants of possession and restitution in one query")
    void listsInOneQuery() {
        for (int i = 1; i <= 5; i++) {
            allocate(enforcement(8012100000000000L + i, EnforcementStatus.CLAIMANT_CONFIRMED, Kind.WARRANT), BAILIFF);
        }
        allocate(enforcement(8012100000000011L, EnforcementStatus.AWAITING_SCHEDULING, Kind.RESTITUTION), "BOB-ID");
        allocate(enforcement(8012100000000021L, EnforcementStatus.CLOSED, Kind.WARRANT), BAILIFF);
        allocate(enforcement(8012100000000022L, EnforcementStatus.AWAITING_REVIEW, Kind.WARRANT), BAILIFF);
        allocate(enforcement(8012100000000023L, EnforcementStatus.CLAIMANT_CONFIRMED, Kind.WRIT), BAILIFF);
        allocate(enforcement(8012100000000024L, EnforcementStatus.CLAIMANT_CONFIRMED, Kind.WARRANT), "alex-id");
        BailiffAllocationEntity ended = allocate(
            enforcement(8012100000000025L, EnforcementStatus.CLAIMANT_CONFIRMED, Kind.WARRANT), BAILIFF);
        ended.setEndedAt(MONDAY);
        entityManager.flush();
        entityManager.clear();

        statistics.clear();
        List<AllocatedWarrant> warrants = batchPrintPlanner.allocatedWarrants("Bob-Id", null, null);

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
        assertThat(warrants).extracting(AllocatedWarrant::caseReference).containsExactly(
            "8012100000000001", "8012100000000002", "8012100000000003", "8012100000000004", "8012100000000005",
            "8012100000000011");
        assertThat(warrants.getFirst()).isEqualTo(new AllocatedWarrant(
            "8012100000000001", "WP-8012100000000001", EnforcementStatus.CLAIMANT_CONFIRMED, EVICTION));
        assertThat(warrants.getLast().status()).isEqualTo(EnforcementStatus.AWAITING_SCHEDULING);
    }

    private enum Kind { WARRANT, RESTITUTION, WRIT }

    private EnforcementOrderEntity enforcement(long caseReference, EnforcementStatus status, Kind kind) {
        PcsCaseEntity pcsCase = PcsCaseEntity.builder().caseReference(caseReference).build();
        pcsCase.addClaim(new ClaimEntity());
        ClaimEntity claim = pcsCaseRepository.save(pcsCase).getClaims().getFirst();

        StatementOfTruthEntity statementOfTruth = new StatementOfTruthEntity();
        statementOfTruth.setAccepted(YesOrNo.YES);
        statementOfTruth.setFullName("Test Name");

        EnforcementOrderEntity enforcement = new EnforcementOrderEntity();
        enforcement.setClaim(claim);
        enforcement.setEnforcementOrder(EnforcementOrder.builder().build());
        enforcement.setStatementOfTruth(statementOfTruthRepository.save(statementOfTruth));
        enforcement.setStatus(status);
        enforcement.setStatusUpdatedAt(MONDAY);
        enforcement.setEnforcementNumber("WP-" + caseReference);
        // Later cases are evicted later, so the list order is the case order.
        enforcement.setBailiffDate(EVICTION.plusMinutes(caseReference % 100 - 1));
        EnforcementOrderEntity saved = enforcementOrderRepository.save(enforcement);
        switch (kind) {
            case WARRANT -> warrantRepository.save(WarrantEntity.builder().enforcementOrder(saved).build());
            case RESTITUTION -> warrantOfRestitutionRepository.save(
                WarrantOfRestitutionEntity.builder().enforcementOrder(saved).build());
            case WRIT -> { }
        }
        return saved;
    }

    private BailiffAllocationEntity allocate(EnforcementOrderEntity enforcement, String bailiffUserId) {
        return bailiffAllocationRepository.save(BailiffAllocationEntity.builder()
                                                    .enforcementOrder(enforcement)
                                                    .bailiffUserId(bailiffUserId)
                                                    .startedAt(MONDAY)
                                                    .build());
    }
}
