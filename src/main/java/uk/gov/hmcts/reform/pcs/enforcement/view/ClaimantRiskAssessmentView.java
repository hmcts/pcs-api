package uk.gov.hmcts.reform.pcs.enforcement.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.enforcement.domain.ClaimantRiskAnswer;
import uk.gov.hmcts.reform.pcs.enforcement.domain.ClaimantRiskAnswers;
import uk.gov.hmcts.reform.pcs.enforcement.domain.ClaimantRiskAssessmentDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.RiskAssessmentUpdateEntity;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffEnforcementRules;
import uk.gov.hmcts.reform.pcs.enforcement.service.ClaimantRiskAssessmentService;
import uk.gov.hmcts.reform.pcs.enforcement.service.CurrentEnforcementService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

/**
 * Shows the claimant's risk assessment for the current warrant on the Risk assessment tab: each question with
 * the original answer and, where the claimant changed it in "Confirm the eviction details", the updated one
 * (HDPI-7552). Set for the same enforcements the tab is shown for, an approved, live warrant.
 */
@Component
@RequiredArgsConstructor
public class ClaimantRiskAssessmentView {

    static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK);

    /**
     * Shown in place of an answer the claimant did not give, for example the details of a risk they no longer
     * report.
     */
    static final String NOT_ANSWERED = "Not answered";

    private final CurrentEnforcementService currentEnforcementService;
    private final ClaimantRiskAssessmentService claimantRiskAssessmentService;

    /**
     * Clears the fields when there is no approved, live warrant, rather than leaving them alone, because a draft
     * "Enforce the order" may have replaced the enforcement order on the case view with an old copy.
     */
    public void setClaimantRiskAssessment(PcsCaseEntity pcsCaseEntity, PCSCase pcsCase) {
        if (pcsCaseEntity == null || pcsCase == null) {
            return;
        }
        Optional<EnforcementOrderEntity> warrant = currentEnforcementService.findCurrentEnforcement(pcsCaseEntity)
            .filter(BailiffEnforcementRules::isApprovedLiveWarrant);
        Optional<ClaimantRiskAnswers> original = warrant.flatMap(claimantRiskAssessmentService::getOriginalAnswers);
        if (original.isEmpty()) {
            if (pcsCase.getEnforcementOrder() != null) {
                pcsCase.getEnforcementOrder().setClaimantRiskAssessment(null);
            }
            return;
        }
        EnforcementOrderEntity enforcement = warrant.get();
        Optional<RiskAssessmentUpdateEntity> latestUpdate = claimantRiskAssessmentService.getLatestUpdate(enforcement);
        ClaimantRiskAnswers updated = latestUpdate.map(RiskAssessmentUpdateEntity::getAnswers).orElse(null);

        enforcementOrderOf(pcsCase).setClaimantRiskAssessment(ClaimantRiskAssessmentDetails.builder()
            .claimantRiskAnswers(answerRows(original.get(), updated))
            .claimantRiskAnswersGivenOn(givenOn(enforcement.getStatementOfTruth()))
            .claimantRiskAnswersUpdatedOn(latestUpdate.map(update -> formatDate(update.getUpdatedAt())).orElse(null))
            .claimantRiskUpdateToReview(YesOrNo.from(latestUpdate.isPresent()
                && isAfterRiskLevelSet(latestUpdate.get().getUpdatedAt(), enforcement.getRiskLevelSetAt())))
            .build());
    }

    /**
     * One row per question the claimant answered, either time. With no update, every updated answer is empty.
     */
    static List<ListValue<ClaimantRiskAnswer>> answerRows(ClaimantRiskAnswers original, ClaimantRiskAnswers updated) {
        ClaimantRiskAnswers originalAnswers = original.normalised();
        ClaimantRiskAnswers updatedAnswers = updated == null ? null : updated.normalised();
        return Arrays.stream(ClaimantRiskQuestion.values())
            .map(question -> answerRow(question, originalAnswers, updatedAnswers))
            .flatMap(Optional::stream)
            .toList();
    }

    static String formatDate(Instant instant) {
        return DATE_FORMAT.format(instant.atZone(UK_ZONE_ID));
    }

    private static Optional<ListValue<ClaimantRiskAnswer>> answerRow(ClaimantRiskQuestion question,
                                                                     ClaimantRiskAnswers original,
                                                                     ClaimantRiskAnswers updated) {
        String originalAnswer = question.answerIn(original);
        String updatedAnswer = updated == null ? originalAnswer : question.answerIn(updated);
        if (originalAnswer == null && updatedAnswer == null) {
            return Optional.empty();
        }
        boolean changed = !Objects.equals(originalAnswer, updatedAnswer);
        ClaimantRiskAnswer row = ClaimantRiskAnswer.builder()
            .question(question.getQuestion())
            .originalAnswer(originalAnswer == null ? NOT_ANSWERED : originalAnswer)
            .updatedAnswer(changed ? Objects.requireNonNullElse(updatedAnswer, NOT_ANSWERED) : null)
            .answerChanged(YesOrNo.from(changed))
            .build();
        return Optional.of(new ListValue<>(question.name(), row));
    }

    /**
     * The statement of truth is signed when the application is submitted, which is when the original answers were
     * given. Its time is recorded in UTC.
     */
    private static String givenOn(StatementOfTruthEntity statementOfTruth) {
        if (statementOfTruth == null) {
            return null;
        }
        LocalDateTime completed = statementOfTruth.getCompletedDate();
        return completed == null ? null : formatDate(completed.toInstant(ZoneOffset.UTC));
    }

    private static boolean isAfterRiskLevelSet(Instant updatedAt, Instant riskLevelSetAt) {
        return riskLevelSetAt == null || riskLevelSetAt.isBefore(updatedAt);
    }

    private static EnforcementOrder enforcementOrderOf(PCSCase pcsCase) {
        if (pcsCase.getEnforcementOrder() == null) {
            pcsCase.setEnforcementOrder(new EnforcementOrder());
        }
        return pcsCase.getEnforcementOrder();
    }
}
