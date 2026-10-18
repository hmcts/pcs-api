package uk.gov.hmcts.reform.pcs.enforcement.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.StatementOfTruthRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoleService;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoles;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
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
import uk.gov.hmcts.reform.pcs.enforcement.view.RiskAssessmentTabView;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
@SuppressWarnings("deprecation")
@DisplayName("Claimant risk assessment updates (V046)")
class ClaimantRiskAssessmentPersistenceIT extends AbstractPostgresContainerIT {

    private static final String UPDATE_BANNER = "The claimant has updated their risk assessment.";

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
    private RiskAssessmentTabView riskAssessmentTabView;
    @MockitoBean
    private UserRoleService userRoleService;
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

        EnforcementOrder shown = caseView(caseReference);
        assertThat(shown.getRiskAssessmentClaimantMarkdown())
            .contains("Information provided by the claimant on")
            .contains("The claimant updated this information on")
            .contains("<th scope=\"row\" class=\"govuk-table__header\">Does anyone at the property pose a risk to the "
                          + "bailiff?</th><td class=\"govuk-table__cell\">No</td>"
                          + "<td class=\"govuk-table__cell\">Yes</td>")
            .contains("<th scope=\"row\" class=\"govuk-table__header\">Is it difficult to access the property?</th>"
                          + "<td class=\"govuk-table__cell\">No</td><td class=\"govuk-table__cell\"></td>");
        assertThat(shown.getRiskAssessmentInternalMarkdown()).contains(UPDATE_BANNER);
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
        assertThat(caseView(caseReference).getRiskAssessmentClaimantMarkdown()).doesNotContain("Updated answer");
        assertThat(caseView(caseReference).getRiskAssessmentInternalMarkdown()).doesNotContain(UPDATE_BANNER);
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

        assertThat(caseView(caseReference).getRiskAssessmentClaimantMarkdown())
            .contains("<th scope=\"row\" class=\"govuk-table__header\">Is it difficult to access the property?</th>"
                          + "<td class=\"govuk-table__cell\">No</td><td class=\"govuk-table__cell\">Yes</td>");
    }

    @Test
    @DisplayName("setting the risk level after an update stops asking the bailiff to review it")
    void settingTheRiskLevelAfterAnUpdate() {
        long caseReference = 4545676789890101L;
        savedWarrant(caseReference);
        claimantRiskAssessmentService.recordAnswers(caseReference, warrantAnswers(YesNoNotSure.YES, VerticalYesNo.NO));
        flushAndClear();
        assertThat(caseView(caseReference).getRiskAssessmentInternalMarkdown()).contains(UPDATE_BANNER);

        riskLevelService.setRiskLevel(caseReference, RiskLevel.MEDIUM, null);
        flushAndClear();

        assertThat(caseView(caseReference).getRiskAssessmentInternalMarkdown()).doesNotContain(UPDATE_BANNER);
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

    /**
     * The Risk assessment tab as a bailiff sees it.
     */
    private EnforcementOrder caseView(long caseReference) {
        when(userRoleService.getCurrentUserCaseRoles(eq(caseReference), anyCollection()))
            .thenReturn(new UserRoles("user", List.of("bailiff")));
        PCSCase pcsCase = PCSCase.builder().build();
        riskAssessmentTabView.setRiskAssessmentTab(
            pcsCaseRepository.findByCaseReference(caseReference).orElseThrow(), pcsCase, caseReference);
        return pcsCase.getEnforcementOrder();
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
        enforcement.setSubmittedAt(Instant.parse("2026-02-25T10:00:00Z"));
        EnforcementOrderEntity saved = enforcementOrderRepository.save(enforcement);
        warrantRepository.save(WarrantEntity.builder().enforcementOrder(saved).build());
        flushAndClear();
        return enforcementOrderRepository.findById(saved.getId()).orElseThrow();
    }
}
