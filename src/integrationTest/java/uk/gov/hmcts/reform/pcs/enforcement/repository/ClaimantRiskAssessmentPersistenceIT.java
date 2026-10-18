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
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.StatementOfTruthRepository;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.enforcement.domain.ClaimantRiskAnswer;
import uk.gov.hmcts.reform.pcs.enforcement.domain.ClaimantRiskAssessmentDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.domain.RiskLevel;
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.PropertyAccessDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.RiskCategory;
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.RiskDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.AdditionalInformation;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.RawWarrantDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WarrantEntity;
import uk.gov.hmcts.reform.pcs.enforcement.service.ClaimantEvictionConfirmationService;
import uk.gov.hmcts.reform.pcs.enforcement.service.ClaimantRiskAssessmentService;
import uk.gov.hmcts.reform.pcs.enforcement.service.RiskLevelService;
import uk.gov.hmcts.reform.pcs.enforcement.view.ClaimantRiskAssessmentView;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
@SuppressWarnings("deprecation")
@DisplayName("Claimant risk assessment updates (V046)")
class ClaimantRiskAssessmentPersistenceIT extends AbstractPostgresContainerIT {

    private static final String ANY_RISK_QUESTION = "Does anyone living at the property pose a risk to the bailiff?";

    @Autowired
    private EnforcementOrderRepository enforcementOrderRepository;
    @Autowired
    private WarrantRepository warrantRepository;
    @Autowired
    private StatementOfTruthRepository statementOfTruthRepository;
    @Autowired
    private PcsCaseRepository pcsCaseRepository;
    @Autowired
    private ClaimantEvictionConfirmationService claimantEvictionConfirmationService;
    @Autowired
    private ClaimantRiskAssessmentService claimantRiskAssessmentService;
    @Autowired
    private RiskLevelService riskLevelService;
    @Autowired
    private ClaimantRiskAssessmentView claimantRiskAssessmentView;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("confirming with a changed answer saves every answer, keeps the application's and confirms")
    void confirmingWithAChangedAnswer() {
        long caseReference = 1212343456567878L;
        UUID id = savedWarrant(caseReference).getId();

        claimantEvictionConfirmationService.recordConfirmation(
            caseReference, YesOrNo.YES, warrantAnswers(YesNoNotSure.YES, VerticalYesNo.NO));
        flushAndClear();

        assertThat(jdbcTemplate.queryForObject(
            "SELECT count(*) FROM enf_risk_assessment_update WHERE enf_case_id = ?", Integer.class, id)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
            "SELECT answers ->> 'anyRiskToBailiff' FROM enf_risk_assessment_update WHERE enf_case_id = ?",
            String.class, id)).isEqualTo("YES");
        assertThat(jdbcTemplate.queryForObject(
            "SELECT answers -> 'riskCategories' ->> 0 FROM enf_risk_assessment_update WHERE enf_case_id = ?",
            String.class, id)).isEqualTo("VIOLENT_OR_AGGRESSIVE");
        assertThat(jdbcTemplate.queryForObject(
            "SELECT status FROM enf_case WHERE id = ?", String.class, id)).isEqualTo("CLAIMANT_CONFIRMED");

        EnforcementOrderEntity reloaded = enforcementOrderRepository.findById(id).orElseThrow();
        assertThat(claimantRiskAssessmentService.getOriginalAnswers(reloaded).orElseThrow().getAnyRiskToBailiff())
            .isEqualTo(YesNoNotSure.NO);
        assertThat(claimantRiskAssessmentService.getCurrentAnswers(reloaded).orElseThrow().getViolentDetails())
            .isEqualTo("Threw a chair");

        ClaimantRiskAssessmentDetails shown = caseView(caseReference);
        ClaimantRiskAnswer anyRisk = row(shown, ANY_RISK_QUESTION);
        assertThat(anyRisk.getOriginalAnswer()).isEqualTo("No");
        assertThat(anyRisk.getUpdatedAnswer()).isEqualTo("Yes");
        assertThat(row(shown, "Is it difficult to access the property?").getUpdatedAnswer()).isNull();
        assertThat(shown.getClaimantRiskAnswersUpdatedOn()).isNotNull();
        assertThat(shown.getClaimantRiskUpdateToReview()).isEqualTo(YesOrNo.YES);
    }

    @Test
    @DisplayName("confirming without changing an answer saves no update")
    void confirmingWithoutAChange() {
        long caseReference = 2323454567678989L;
        UUID id = savedWarrant(caseReference).getId();

        claimantEvictionConfirmationService.recordConfirmation(
            caseReference, YesOrNo.NO, warrantAnswers(YesNoNotSure.NO, VerticalYesNo.NO));
        flushAndClear();

        assertThat(jdbcTemplate.queryForObject(
            "SELECT count(*) FROM enf_risk_assessment_update WHERE enf_case_id = ?", Integer.class, id)).isZero();
        assertThat(jdbcTemplate.queryForObject(
            "SELECT status FROM enf_case WHERE id = ?", String.class, id)).isEqualTo("CLAIMANT_UNABLE_TO_ATTEND");
        assertThat(caseView(caseReference).getClaimantRiskUpdateToReview()).isEqualTo(YesOrNo.NO);
    }

    @Test
    @DisplayName("each change is kept, the latest is pre-filled and compared against the application")
    void keepsEveryChange() {
        long caseReference = 3434565678789090L;
        final UUID id = savedWarrant(caseReference).getId();

        claimantRiskAssessmentService.recordAnswers(caseReference, warrantAnswers(YesNoNotSure.YES, VerticalYesNo.NO));
        flushAndClear();
        claimantRiskAssessmentService.recordAnswers(caseReference, warrantAnswers(YesNoNotSure.YES, VerticalYesNo.YES));
        flushAndClear();

        assertThat(jdbcTemplate.queryForList(
            "SELECT answers ->> 'isDifficultToAccessProperty' FROM enf_risk_assessment_update WHERE enf_case_id = ? "
                + "ORDER BY updated_at", String.class, id)).containsExactly("NO", "YES");

        EnforcementOrder eventData = new EnforcementOrder();
        claimantRiskAssessmentService.prefillAnswers(caseReference, eventData);
        assertThat(eventData.getWarrantDetails().getPropertyAccessDetails().getIsDifficultToAccessProperty())
            .isEqualTo(VerticalYesNo.YES);
        assertThat(eventData.getChooseEnforcementType().getValueCode()).isEqualTo("WARRANT");

        assertThat(row(caseView(caseReference), "Is it difficult to access the property?"))
            .extracting(ClaimantRiskAnswer::getOriginalAnswer, ClaimantRiskAnswer::getUpdatedAnswer)
            .containsExactly("No", "Yes");
    }

    @Test
    @DisplayName("setting the risk level after an update stops asking the bailiff to review it")
    void settingTheRiskLevelAfterAnUpdate() {
        long caseReference = 4545676789890101L;
        savedWarrant(caseReference);
        claimantRiskAssessmentService.recordAnswers(caseReference, warrantAnswers(YesNoNotSure.YES, VerticalYesNo.NO));
        flushAndClear();
        assertThat(caseView(caseReference).getClaimantRiskUpdateToReview()).isEqualTo(YesOrNo.YES);

        riskLevelService.setRiskLevel(caseReference, RiskLevel.MEDIUM, null);
        flushAndClear();

        assertThat(caseView(caseReference).getClaimantRiskUpdateToReview()).isEqualTo(YesOrNo.NO);
    }

    @Test
    @DisplayName("updates are removed with their enforcement")
    void removedWithTheEnforcement() {
        long caseReference = 5656787890901212L;
        UUID id = savedWarrant(caseReference).getId();
        claimantRiskAssessmentService.recordAnswers(caseReference, warrantAnswers(YesNoNotSure.YES, VerticalYesNo.NO));
        flushAndClear();

        jdbcTemplate.update("DELETE FROM enf_case WHERE id = ?", id);

        assertThat(jdbcTemplate.queryForObject(
            "SELECT count(*) FROM enf_risk_assessment_update WHERE enf_case_id = ?", Integer.class, id)).isZero();
    }

    private ClaimantRiskAssessmentDetails caseView(long caseReference) {
        PCSCase pcsCase = PCSCase.builder().build();
        claimantRiskAssessmentView.setClaimantRiskAssessment(
            pcsCaseRepository.findByCaseReference(caseReference).orElseThrow(), pcsCase);
        return pcsCase.getEnforcementOrder().getClaimantRiskAssessment();
    }

    private static ClaimantRiskAnswer row(ClaimantRiskAssessmentDetails details, String question) {
        return details.getClaimantRiskAnswers().stream()
            .map(ListValue::getValue)
            .filter(row -> row.getQuestion().equals(question))
            .findFirst()
            .orElseThrow();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private static EnforcementOrder warrantAnswers(YesNoNotSure anyRisk, VerticalYesNo difficultToAccess) {
        DynamicStringListElement warrant = new DynamicStringListElement("WARRANT", "Warrant of possession");
        boolean risk = anyRisk == YesNoNotSure.YES;
        return EnforcementOrder.builder()
            .chooseEnforcementType(new DynamicStringList(warrant, List.of(warrant)))
            .warrantDetails(WarrantDetails.builder()
                .anyRiskToBailiff(anyRisk)
                .riskCategories(risk ? Set.of(RiskCategory.VIOLENT_OR_AGGRESSIVE) : null)
                .riskDetails(risk ? RiskDetails.builder().violentDetails("Threw a chair").build() : null)
                .propertyAccessDetails(PropertyAccessDetails.builder()
                    .isDifficultToAccessProperty(difficultToAccess)
                    .clarificationOnAccessDifficultyText(
                        difficultToAccess == VerticalYesNo.YES ? "Locked gate" : null)
                    .build())
                .additionalInformation(AdditionalInformation.builder()
                    .additionalInformationSelect(VerticalYesNo.NO)
                    .build())
                .build())
            .rawWarrantDetails(RawWarrantDetails.builder().vulnerablePeoplePresent(YesNoNotSure.NO).build())
            .build();
    }

    private EnforcementOrderEntity savedWarrant(long caseReference) {
        PcsCaseEntity pcsCase = PcsCaseEntity.builder().caseReference(caseReference).build();
        pcsCase.addClaim(new ClaimEntity());
        final ClaimEntity claim = pcsCaseRepository.save(pcsCase).getClaims().getFirst();

        StatementOfTruthEntity statementOfTruth = new StatementOfTruthEntity();
        statementOfTruth.setAccepted(YesOrNo.YES);
        statementOfTruth.setFullName("Test Name");
        statementOfTruth.setCompletedDate(LocalDateTime.parse("2026-02-25T10:00:00"));

        EnforcementOrderEntity enforcement = new EnforcementOrderEntity();
        enforcement.setClaim(claim);
        enforcement.setEnforcementOrder(warrantAnswers(YesNoNotSure.NO, VerticalYesNo.NO));
        enforcement.setStatementOfTruth(statementOfTruthRepository.save(statementOfTruth));
        enforcement.setStatus(EnforcementStatus.AWAITING_CLAIMANT_CONFIRMATION);
        enforcement.setStatusUpdatedAt(Instant.now());
        EnforcementOrderEntity saved = enforcementOrderRepository.save(enforcement);
        warrantRepository.save(WarrantEntity.builder().enforcementOrder(saved).build());
        flushAndClear();
        return enforcementOrderRepository.findById(saved.getId()).orElseThrow();
    }
}
