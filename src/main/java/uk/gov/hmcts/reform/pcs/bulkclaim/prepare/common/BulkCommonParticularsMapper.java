package uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimExtras.PaymentHistoryLine;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.ParticularsOfClaimType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.PaymentHistoryType;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalReasons;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.List;

/**
 * Maps the particulars of claim that rental and mortgage claims share.
 */
@Component
public class BulkCommonParticularsMapper {

    /**
     * Data from the common particulars that has no home on the case and goes into the claim extras.
     */
    public record CommonParticulars(Boolean claimingCosts) {
    }

    /**
     * Sets pre-action protocol (England only), language, application with claim, defendant circumstances and
     * additional reasons on the case.
     *
     * @param path path of the particulars element relative to the claim, for example {@code ClaimParticulars}
     */
    public CommonParticulars apply(ParticularsOfClaimType particulars,
                                   LegislativeCountry country,
                                   PCSCase target,
                                   String path,
                                   BulkErrorCollector errors) {

        if (country == LegislativeCountry.ENGLAND) {
            applyPreActionProtocol(particulars, target, path, errors);
        }

        target.setLanguageUsed(LanguageUsed.valueOf(particulars.getLanguageUsed().name()));
        target.setApplicationWithClaim(VerticalYesNo.from(Boolean.TRUE.equals(particulars.isApplicationWithClaim())));

        applyDefendantCircumstances(particulars, target, path, errors);
        applyAdditionalReasons(particulars, target, path, errors);

        return new CommonParticulars(particulars.isClaimingCosts());
    }

    public List<PaymentHistoryLine> paymentHistory(PaymentHistoryType paymentHistory) {
        if (paymentHistory == null) {
            return List.of();
        }
        return paymentHistory.getPaymentLine().stream()
            .map(line -> new PaymentHistoryLine(
                line.getPaymentDueDate(), line.getAmountDue(), line.getAmountPaid(), line.getCumulativeArrears()))
            .toList();
    }

    private void applyPreActionProtocol(ParticularsOfClaimType particulars,
                                        PCSCase target,
                                        String path,
                                        BulkErrorCollector errors) {
        boolean completed = particulars.isPreActionProtocolCompleted();
        target.setPreActionProtocolCompleted(VerticalYesNo.from(completed));
        if (!completed) {
            String explanationPath = path + "/PreActionProtocolIncompleteExplanation";
            String explanation = particulars.getPreActionProtocolIncompleteExplanation();
            if (errors.required(explanationPath, explanation,
                                PCSCase.PRE_ACTION_PROTOCOL_INCOMPLETE_EXPLANATION_LABEL)) {
                errors.maxLength(explanationPath, explanation, TextAreaValidationService.SHORT_TEXT_LIMIT,
                                 PCSCase.PRE_ACTION_PROTOCOL_INCOMPLETE_EXPLANATION_LABEL);
            }
            target.setPreActionProtocolIncompleteExplanation(explanation);
        }
    }

    private void applyDefendantCircumstances(ParticularsOfClaimType particulars,
                                             PCSCase target,
                                             String path,
                                             BulkErrorCollector errors) {
        String circumstances = StringUtils.trimToNull(particulars.getDefendantsCircumstances());
        errors.maxLength(path + "/DefendantsCircumstances", circumstances, BulkLimits.LONG_TEXT,
                         "Defendants' circumstances");
        target.setDefendantCircumstances(DefendantCircumstances.builder()
            .hasDefendantCircumstancesInfo(VerticalYesNo.from(circumstances != null))
            .defendantCircumstancesInfo(circumstances)
            .build());
    }

    private void applyAdditionalReasons(ParticularsOfClaimType particulars,
                                        PCSCase target,
                                        String path,
                                        BulkErrorCollector errors) {
        String reasons = StringUtils.trimToNull(particulars.getReasonForPossession());
        errors.maxLength(path + "/ReasonForPossession", reasons, TextAreaValidationService.EXTRA_LONG_TEXT_LIMIT,
                         "Additional reasons for possession");
        target.setAdditionalReasonsForPossession(AdditionalReasons.builder()
            .hasReasons(VerticalYesNo.from(reasons != null))
            .reasons(reasons)
            .build());
    }
}
