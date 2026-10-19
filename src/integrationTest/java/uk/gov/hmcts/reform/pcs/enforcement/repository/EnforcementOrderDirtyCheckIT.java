package uk.gov.hmcts.reform.pcs.enforcement.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.instancio.Instancio;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.domain.RiskLevel;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantofrestitution.WarrantOfRestitutionDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hibernate dirty-checks the enforcement_order JSON column by comparing the loaded value with a JSON round-trip
 * copy of it, so a value graph that is not equal to its own round trip would make every loaded enf_case row
 * issue an UPDATE on flush and overwrite concurrent writes.
 */
@SpringBootTest
@ActiveProfiles("integration")
@Transactional
@DisplayName("Enforcement order dirty checking")
class EnforcementOrderDirtyCheckIT extends AbstractPostgresContainerIT {

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
    private ObjectMapper objectMapper;

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
    @DisplayName("does not update an empty enforcement order that was only loaded")
    void doesNotUpdateUnchangedEmptyEnforcementOrder() {
        UUID id = save(1000000000000011L, EnforcementOrder.builder().build());

        assertNoUpdateOnLoad(id);
    }

    @Test
    @DisplayName("does not update an application as submitted that was only loaded")
    void doesNotUpdateUnchangedSubmittedEnforcementOrder() {
        EnforcementOrder submitted = EnforcementOrder.builder()
            .chooseEnforcementType(DynamicStringList.builder()
                                       .value(DynamicStringListElement.builder()
                                                  .code(SelectEnforcementType.WARRANT.name())
                                                  .build())
                                       .build())
            .warrantDetails(WarrantDetails.builder().build())
            .writDetails(WritDetails.builder().build())
            .warrantOfRestitutionDetails(WarrantOfRestitutionDetails.builder().build())
            .build();
        UUID id = save(1000000000000012L, submitted);

        assertNoUpdateOnLoad(id);
    }

    @Test
    @DisplayName("does not update a fully populated enforcement order that was only loaded")
    void doesNotUpdateUnchangedPopulatedEnforcementOrder() {
        UUID id = save(1000000000000013L, Instancio.create(EnforcementOrder.class));

        assertNoUpdateOnLoad(id);
    }

    /**
     * Reading an order materialises every {@code @JsonUnwrapped} nested object the application left null, so an
     * order as the journey builds it is never equal to its own round trip and would be dirty the moment it was
     * inserted. {@code EnforcementOrderService} stores the round trip for that reason; this is the assumption
     * that makes it work, namely that Hibernate's snapshot agrees with the service's ObjectMapper.
     */
    @Test
    @DisplayName("does not update a freshly inserted enforcement order stored in its round-trip form")
    void doesNotUpdateFreshlyInsertedEnforcementOrder() {
        EnforcementOrder submitted = EnforcementOrder.builder()
            .chooseEnforcementType(DynamicStringList.builder()
                                       .value(DynamicStringListElement.builder()
                                                  .code(SelectEnforcementType.WARRANT.name())
                                                  .build())
                                       .build())
            .warrantDetails(WarrantDetails.builder().build())
            .writDetails(WritDetails.builder().build())
            .warrantOfRestitutionDetails(WarrantOfRestitutionDetails.builder().build())
            .build();

        insert(1000000000000015L, objectMapper.convertValue(submitted, EnforcementOrder.class));

        statistics.clear();
        entityManager.flush();
        assertThat(statistics.getEntityUpdateCount()).isZero();
    }

    @Test
    @DisplayName("persists a change made in place to a nested field of the enforcement order")
    void persistsInPlaceChangeToNestedField() {
        EnforcementOrder enforcementOrder = Instancio.create(EnforcementOrder.class);
        enforcementOrder.getWarrantDetails().getAdditionalInformation().setAdditionalInformationDetails("before");
        UUID id = save(1000000000000014L, enforcementOrder);

        statistics.clear();
        enforcementOrderRepository.findById(id).orElseThrow()
            .getEnforcementOrder().getWarrantDetails().getAdditionalInformation()
            .setAdditionalInformationDetails("after");
        entityManager.flush();
        entityManager.clear();

        assertThat(statistics.getEntityUpdateCount()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
            "SELECT enforcement_order ->> 'warrantAdditionalInformationDetails' FROM enf_case WHERE id = ?",
            String.class, id)).isEqualTo("after");
        assertThat(enforcementOrderRepository.findById(id).orElseThrow()
                       .getEnforcementOrder().getWarrantDetails().getAdditionalInformation()
                       .getAdditionalInformationDetails()).isEqualTo("after");
    }

    private void assertNoUpdateOnLoad(UUID id) {
        final String storedBefore = storedEnforcementOrder(id);

        statistics.clear();
        enforcementOrderRepository.findById(id).orElseThrow();
        entityManager.flush();

        assertThat(statistics.getEntityUpdateCount()).isZero();
        assertThat(storedEnforcementOrder(id)).isEqualTo(storedBefore);
    }

    private String storedEnforcementOrder(UUID id) {
        return jdbcTemplate.queryForObject(
            "SELECT enforcement_order::text FROM enf_case WHERE id = ?", String.class, id);
    }

    private UUID save(long caseReference, EnforcementOrder enforcementOrder) {
        UUID id = insert(caseReference, enforcementOrder);
        entityManager.clear();
        return id;
    }

    private UUID insert(long caseReference, EnforcementOrder enforcementOrder) {
        StatementOfTruthEntity statementOfTruth = new StatementOfTruthEntity();
        statementOfTruth.setAccepted(YesOrNo.YES);
        statementOfTruth.setFullName("Test Name");

        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        EnforcementOrderEntity entity = new EnforcementOrderEntity();
        entity.setClaim(savedClaim(caseReference));
        entity.setEnforcementOrder(enforcementOrder);
        entity.setStatementOfTruth(statementOfTruthRepository.save(statementOfTruth));
        entity.setStatus(EnforcementStatus.AWAITING_SCHEDULING);
        entity.setStatusUpdatedAt(now);
        entity.setEnforcementNumber("WA123456");
        entity.setApprovedAt(now);
        entity.setBailiffDate(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));
        entity.setRiskLevel(RiskLevel.MEDIUM);
        entity.setRiskLevelSetAt(now);
        UUID id = enforcementOrderRepository.save(entity).getId();
        entityManager.flush();
        return id;
    }

    private ClaimEntity savedClaim(long caseReference) {
        PcsCaseEntity pcsCase = PcsCaseEntity.builder().caseReference(caseReference).build();
        ClaimEntity claim = new ClaimEntity();
        pcsCase.addClaim(claim);
        return pcsCaseRepository.save(pcsCase).getClaims().getFirst();
    }
}
