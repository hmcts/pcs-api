package uk.gov.hmcts.reform.pcs.enforcement.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.StatementOfTruthRepository;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.event.EnforcementSubmitEvent;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.exception.LiveEnforcementExistsException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
@DisplayName("Enforcement order persistence")
class EnforcementOrderPersistenceIT extends AbstractPostgresContainerIT {

    @Autowired
    private EnforcementOrderRepository enforcementOrderRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private StatementOfTruthRepository statementOfTruthRepository;

    @Autowired
    private PcsCaseRepository pcsCaseRepository;

    @Autowired
    private EnforcementSubmitEvent enforcementSubmitEvent;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("links already-saved documents to the enforcement order through enf_case_id")
    void linksDocumentsToEnforcementOrder() {
        ClaimEntity claim = savedClaim(1234123412341234L);
        List<DocumentEntity> documents = documentRepository.saveAll(List.of(
            DocumentEntity.builder().fileName("first.pdf").url("http://dm/first").build(),
            DocumentEntity.builder().fileName("second.pdf").url("http://dm/second").build()
        ));

        EnforcementOrderEntity enforcementOrder = enforcementOrder(claim);
        enforcementOrder.addDocuments(documents);
        UUID enforcementOrderId = enforcementOrderRepository.save(enforcementOrder).getId();
        entityManager.flush();
        entityManager.clear();

        for (DocumentEntity document : documents) {
            UUID enfCaseId = jdbcTemplate.queryForObject(
                "SELECT enf_case_id FROM document WHERE id = ?", UUID.class, document.getId());
            assertThat(enfCaseId).isEqualTo(enforcementOrderId);
        }
        EnforcementOrderEntity reloaded = enforcementOrderRepository.findById(enforcementOrderId).orElseThrow();
        assertThat(reloaded.getDocuments())
            .extracting(DocumentEntity::getFileName)
            .containsExactlyInAnyOrder("first.pdf", "second.pdf");
    }

    @Test
    @DisplayName("finds enforcement orders by claim id")
    void findsEnforcementOrdersByClaimId() {
        ClaimEntity claim = savedClaim(4321432143214321L);
        ClaimEntity otherClaim = savedClaim(5678567856785678L);
        final UUID enforcementOrderId = enforcementOrderRepository.save(enforcementOrder(claim)).getId();
        enforcementOrderRepository.save(enforcementOrder(otherClaim));
        entityManager.flush();
        entityManager.clear();

        assertThat(enforcementOrderRepository.findByClaimId(claim.getId()))
            .extracting(EnforcementOrderEntity::getId)
            .containsExactly(enforcementOrderId);
    }

    @Test
    @DisplayName("refuses a submitted application while the claim already has a live one")
    void refusesASecondLiveEnforcementOnSubmit() {
        long caseReference = 6789678967896789L;
        ClaimEntity claim = savedClaim(caseReference);
        EnforcementOrderEntity live = enforcementOrder(claim);
        live.setStatus(EnforcementStatus.AWAITING_REVIEW);
        live.setStatusUpdatedAt(Instant.now());
        final UUID liveId = enforcementOrderRepository.save(live).getId();
        entityManager.flush();
        entityManager.clear();

        SubmitResponse<State> response = enforcementSubmitEvent.submit(submitPayload(caseReference));
        entityManager.flush();
        entityManager.clear();

        assertThat(response.getErrors()).containsExactly(LiveEnforcementExistsException.MESSAGE);
        assertThat(response.getState()).isNull();
        assertThat(enforcementOrderRepository.findByClaimId(claim.getId()))
            .extracting(EnforcementOrderEntity::getId)
            .containsExactly(liveId);
    }

    private static EventPayload<PCSCase, State> submitPayload(long caseReference) {
        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
            .chooseEnforcementType(DynamicStringList.builder()
                                       .value(DynamicStringListElement.builder()
                                                  .code(SelectEnforcementType.WARRANT.name())
                                                  .build())
                                       .build())
            .build();
        return new EventPayload<>(caseReference,
                                  PCSCase.builder().enforcementOrder(enforcementOrder).build(),
                                  null);
    }

    private ClaimEntity savedClaim(long caseReference) {
        PcsCaseEntity pcsCase = PcsCaseEntity.builder().caseReference(caseReference).build();
        ClaimEntity claim = new ClaimEntity();
        pcsCase.addClaim(claim);
        return pcsCaseRepository.save(pcsCase).getClaims().getFirst();
    }

    private EnforcementOrderEntity enforcementOrder(ClaimEntity claim) {
        StatementOfTruthEntity statementOfTruth = new StatementOfTruthEntity();
        statementOfTruth.setAccepted(YesOrNo.YES);
        statementOfTruth.setFullName("Test Name");

        EnforcementOrderEntity enforcementOrder = new EnforcementOrderEntity();
        enforcementOrder.setClaim(claim);
        enforcementOrder.setEnforcementOrder(EnforcementOrder.builder().build());
        enforcementOrder.setStatementOfTruth(statementOfTruthRepository.save(statementOfTruth));
        return enforcementOrder;
    }
}
