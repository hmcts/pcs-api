package uk.gov.hmcts.reform.pcs.enforcement.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.enforcement.document.EnforcementDocumentScheduler;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantofrestitution.WarrantOfRestitutionDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementOrderService;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementStatusTransitions;
import uk.gov.hmcts.reform.pcs.enforcement.service.strategy.EnforcementTypeStrategyFactory;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Submits a real application of each enforcement type against Postgres. Three of the four strategies insert the
 * type's own row with a non-null enf_case_id of their own, so only a persistent enforcement will do; every
 * strategy unit test mocks the repositories and so cannot see that.
 */
@SpringBootTest
@ActiveProfiles("integration")
@Transactional
@DisplayName("Enforcement application submit persistence")
class EnforcementSubmitPersistenceIT extends AbstractPostgresContainerIT {

    @Autowired
    private EnforcementOrderRepository enforcementOrderRepository;

    @Autowired
    private PcsCaseRepository pcsCaseRepository;

    @Autowired
    private PcsCaseService pcsCaseService;

    @Autowired
    private EnforcementTypeStrategyFactory strategyFactory;

    @Autowired
    private EnforcementStatusTransitions enforcementStatusTransitions;

    @Autowired
    private EnforcementDocumentScheduler enforcementDocumentScheduler;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private EnforcementOrderService enforcementOrderService;

    @BeforeEach
    void setUp() {
        // Deleting the draft needs the caller's IDAM user and organisation, which this test has no use for.
        enforcementOrderService = new EnforcementOrderService(
            enforcementOrderRepository, pcsCaseService, mock(DraftCaseDataService.class), strategyFactory,
            enforcementStatusTransitions, enforcementDocumentScheduler);
    }

    @ParameterizedTest
    @CsvSource({
        "WARRANT, 1000000000000001, enf_warrant",
        "WRIT, 1000000000000002, enf_writ",
        "WARRANT_OF_RESTITUTION, 1000000000000003, enf_warrant_of_restitution",
        "WRIT_OF_RESTITUTION, 1000000000000004, enf_writ_of_restitution",
    })
    @DisplayName("stores the application and the type's own row, pointing back at the enforcement")
    void shouldStoreTheTypesOwnRowAgainstTheEnforcement(SelectEnforcementType enforcementType,
                                                        long caseReference,
                                                        String childTable) {
        final ClaimEntity claim = savedClaim(caseReference);

        enforcementOrderService.saveAndClearDraftData(caseReference, enforcementOrder(enforcementType));
        entityManager.flush();
        entityManager.clear();

        List<EnforcementOrderEntity> enforcements = enforcementOrderRepository.findByClaimId(claim.getId());
        assertThat(enforcements).hasSize(1);
        UUID enforcementId = enforcements.getFirst().getId();
        assertThat(jdbcTemplate.queryForList(
            "SELECT enf_case_id FROM " + childTable, UUID.class))
            .containsExactly(enforcementId);
    }

    private static EnforcementOrder enforcementOrder(SelectEnforcementType enforcementType) {
        return EnforcementOrder.builder()
            .chooseEnforcementType(DynamicStringList.builder()
                                       .value(DynamicStringListElement.builder()
                                                  .code(enforcementType.name())
                                                  .build())
                                       .build())
            .warrantDetails(WarrantDetails.builder().build())
            .writDetails(WritDetails.builder().build())
            .warrantOfRestitutionDetails(WarrantOfRestitutionDetails.builder().build())
            .build();
    }

    private ClaimEntity savedClaim(long caseReference) {
        PcsCaseEntity pcsCase = PcsCaseEntity.builder().caseReference(caseReference).build();
        ClaimEntity claim = new ClaimEntity();
        pcsCase.addClaim(claim);
        return pcsCaseRepository.save(pcsCase).getClaims().getFirst();
    }
}
