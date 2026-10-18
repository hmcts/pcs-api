package uk.gov.hmcts.reform.pcs.enforcement.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.SearchCriteria;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.StatementOfTruthRepository;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.ccd.view.globalsearch.SearchCriteriaIndexer;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementDecisionService;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
@DisplayName("Warrant and writ numbers in global search (HDPI-7677)")
class EnforcementGlobalSearchReferencesIT extends AbstractPostgresContainerIT {

    @Autowired
    private SearchCriteriaIndexer searchCriteriaIndexer;

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
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("indexes every warrant and writ number given on the case, closed or live, oldest first")
    void indexesEveryNumberGivenOnTheCase() {
        long caseReference = 1818181818181818L;
        ClaimEntity claim = savedClaim(caseReference);

        UUID firstWarrant = save(claim, SelectEnforcementType.WARRANT, EnforcementStatus.AWAITING_REVIEW);
        final String firstWarrantNumber = enforcementDecisionService.accept(caseReference).orElseThrow()
            .enforcementNumber();
        flushAndClear();
        jdbcTemplate.update("UPDATE enf_case SET status = 'CLOSED' WHERE id = ?", firstWarrant);

        save(claim, SelectEnforcementType.WRIT, EnforcementStatus.AWAITING_REVIEW);
        final String writNumber = enforcementDecisionService.accept(caseReference).orElseThrow().enforcementNumber();
        flushAndClear();

        save(claim, SelectEnforcementType.WARRANT, EnforcementStatus.AWAITING_REVIEW);
        enforcementDecisionService.reject(caseReference, "The possession order was set aside");
        flushAndClear();

        SearchCriteria searchCriteria = searchCriteriaIndexer.buildSearchCriteria(PCSCase.builder().build(),
                                                                                  caseReference);

        assertThat(searchCriteria.getOtherCaseReferences())
            .extracting(ListValue::getValue)
            .containsExactly(firstWarrantNumber, writNumber);
        assertThat(firstWarrantNumber).matches("WP-\\d{8}");
        assertThat(writNumber).matches("HP-\\d{8}");
    }

    @Test
    @DisplayName("does not index another case's numbers, or anything before a number is given")
    void indexesOnlyTheCasesOwnNumbers() {
        long otherCaseReference = 1919191919191919L;
        save(savedClaim(otherCaseReference), SelectEnforcementType.WARRANT, EnforcementStatus.AWAITING_REVIEW);
        enforcementDecisionService.accept(otherCaseReference).orElseThrow();
        flushAndClear();

        long caseReference = 2020202020202020L;
        save(savedClaim(caseReference), SelectEnforcementType.WARRANT, EnforcementStatus.AWAITING_REVIEW);

        SearchCriteria searchCriteria = searchCriteriaIndexer.buildSearchCriteria(PCSCase.builder().build(),
                                                                                  caseReference);

        assertThat(searchCriteria.getOtherCaseReferences()).isNull();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private UUID save(ClaimEntity claim, SelectEnforcementType type, EnforcementStatus status) {
        StatementOfTruthEntity statementOfTruth = new StatementOfTruthEntity();
        statementOfTruth.setAccepted(YesOrNo.YES);
        statementOfTruth.setFullName("Test Name");

        EnforcementOrderEntity enforcementOrder = new EnforcementOrderEntity();
        enforcementOrder.setClaim(entityManager.getReference(ClaimEntity.class, claim.getId()));
        enforcementOrder.setEnforcementOrder(EnforcementOrder.builder()
                                                 .chooseEnforcementType(DynamicStringList.builder()
                                                     .value(DynamicStringListElement.builder()
                                                                .code(type.name())
                                                                .label(type.getLabel())
                                                                .build())
                                                     .build())
                                                 .build());
        enforcementOrder.setStatementOfTruth(statementOfTruthRepository.save(statementOfTruth));
        enforcementOrder.setStatus(status);
        enforcementOrder.setStatusUpdatedAt(Instant.now());
        UUID id = enforcementOrderRepository.save(enforcementOrder).getId();
        flushAndClear();
        return id;
    }

    private ClaimEntity savedClaim(long caseReference) {
        PcsCaseEntity pcsCase = PcsCaseEntity.builder().caseReference(caseReference).build();
        ClaimEntity claim = new ClaimEntity();
        pcsCase.addClaim(claim);
        return pcsCaseRepository.save(pcsCase).getClaims().getFirst();
    }
}
