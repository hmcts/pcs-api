package uk.gov.hmcts.reform.pcs.enforcement.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.CaseType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.enforcement.accesscontrol.EnforcementViewer;
import uk.gov.hmcts.reform.pcs.enforcement.accesscontrol.EnforcementViewerService;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.RiskAssessmentUpdateEntity;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffEnforcementRules;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffNoteService;
import uk.gov.hmcts.reform.pcs.enforcement.service.ClaimantRiskAssessmentService;
import uk.gov.hmcts.reform.pcs.enforcement.service.CurrentEnforcementService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Sets the "Risk assessment" tab for the case view (13-risk-assessment-tab.feature, HDPI-7350).
 *
 * <p>The tab is for the claim's most recent warrant or warrant of restitution that a caseworker has approved:
 * the current enforcement when it is one, as for the bailiff events. It stays once the warrant closes, so that
 * its notes and risk level can still be read (OPEN-QUESTIONS 63). It is never shown for a writ, or before a
 * warrant has been approved.
 *
 * <p>Defendants never see it (HDPI-7539). A defendant's legal representative holds the same
 * caseworker-pcs-solicitor IDAM role as the claimant's, which the claimant side's tab is granted on, so for a
 * user on the defendant side, or whose side of the case cannot be established ({@link EnforcementViewerService}),
 * the tab is hidden and left empty.
 */
@Component
@RequiredArgsConstructor
public class RiskAssessmentTabView {

    private final CurrentEnforcementService currentEnforcementService;
    private final BailiffNoteService bailiffNoteService;
    private final EnforcementViewerService enforcementViewerService;
    private final ClaimantRiskAssessmentService claimantRiskAssessmentService;

    /**
     * Every field is set either way rather than left alone, because a draft "Enforce the order" may have
     * replaced the enforcement order on the case view with an old copy. A claim with no generated warrant is
     * left without an enforcement order if it has none, rather than given one that only says "no".
     */
    public void setRiskAssessmentTab(PcsCaseEntity pcsCaseEntity, PCSCase pcsCase, long caseReference) {
        if (pcsCaseEntity == null || pcsCase == null) {
            return;
        }
        Optional<EnforcementOrderEntity> warrant = currentEnforcementService
            .findEnforcementsCurrentFirst(pcsCaseEntity).stream()
            .filter(BailiffEnforcementRules::isGeneratedWarrant)
            .findFirst();
        EnforcementViewer viewer = warrant.isEmpty()
            ? null
            : enforcementViewerService.currentViewer(pcsCaseEntity, pcsCase, caseReference);
        if (viewer == null || viewer.isRestricted()) {
            EnforcementOrder enforcementOrder = pcsCase.getEnforcementOrder();
            if (enforcementOrder != null) {
                enforcementOrder.setShowRiskAssessmentTab(YesOrNo.NO);
                setMarkdown(enforcementOrder, null, null, null);
            }
            return;
        }
        EnforcementOrderEntity enforcement = warrant.get();
        EnforcementOrder enforcementOrder = enforcementOrderOf(pcsCase);
        enforcementOrder.setShowRiskAssessmentTab(YesOrNo.YES);
        RiskAssessmentTabMarkdown.ClaimantAnswers application = claimantAnswers(enforcement);
        Optional<RiskAssessmentUpdateEntity> update = claimantRiskAssessmentService.getLatestUpdate(enforcement);
        RiskAssessmentTabMarkdown.ClaimantAnswers original = update.isEmpty()
            ? application
            : claimantRiskAssessmentService.getOriginalAnswers(enforcement)
                .map(answers -> RiskAssessmentTabMarkdown.withRiskAnswers(application, answers))
                .orElse(application);
        RiskAssessmentTabMarkdown.ClaimantAnswers updated = update
            .map(latest -> RiskAssessmentTabMarkdown.withRiskAnswers(application, latest.getAnswers().normalised()))
            .orElse(null);
        Instant updatedAt = update.map(RiskAssessmentUpdateEntity::getUpdatedAt).orElse(null);

        String internal = RiskAssessmentTabMarkdown.internalSections(
            bailiffNoteService.getNotes(enforcement), enforcement.getRiskLevel(), enforcement.getRiskLevelReason());
        if (updated != null && isAfterRiskLevelSet(updatedAt, enforcement.getRiskLevelSetAt())) {
            internal = TabMarkup.lines(List.of(RiskAssessmentTabMarkdown.updateBanner(original, updated), internal));
        }
        setMarkdown(
            enforcementOrder,
            RiskAssessmentTabMarkdown.printButton(CaseType.getJurisdictionId(), CaseType.getCaseType(), caseReference),
            internal,
            RiskAssessmentTabMarkdown.claimantSection(
                original, enforcement.getSubmittedAt(), updated, updatedAt, viewer == EnforcementViewer.CLAIMANT)
        );
    }

    /**
     * The claimant's change waits for the bailiff's review until the risk level is set after it (HDPI-7552).
     */
    private static boolean isAfterRiskLevelSet(Instant updatedAt, Instant riskLevelSetAt) {
        return riskLevelSetAt == null || riskLevelSetAt.isBefore(updatedAt);
    }

    private static RiskAssessmentTabMarkdown.ClaimantAnswers claimantAnswers(EnforcementOrderEntity enforcement) {
        EnforcementOrder application = Optional.ofNullable(enforcement.getEnforcementOrder())
            .orElseGet(EnforcementOrder::new);
        boolean restitution = SelectEnforcementType.from(application) == SelectEnforcementType.WARRANT_OF_RESTITUTION
            || enforcement.getWarrantOfRestitutionDetails() != null;
        return RiskAssessmentTabMarkdown.answersOf(application, restitution);
    }

    private static void setMarkdown(EnforcementOrder enforcementOrder, String print, String internal,
                                    String claimant) {
        enforcementOrder.setRiskAssessmentPrintMarkdown(print);
        enforcementOrder.setRiskAssessmentInternalMarkdown(internal);
        enforcementOrder.setRiskAssessmentClaimantMarkdown(claimant);
    }

    private static EnforcementOrder enforcementOrderOf(PCSCase pcsCase) {
        if (pcsCase.getEnforcementOrder() == null) {
            pcsCase.setEnforcementOrder(new EnforcementOrder());
        }
        return pcsCase.getEnforcementOrder();
    }
}
