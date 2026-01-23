package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RepaymentCosts;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.StatementOfTruthDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

@AllArgsConstructor
@Component
public class StatementOfTruthPage implements CcdPageConfiguration {

    private static final String WARRANT_COMPLETED_BY_CLAIMANT = "warrantCompletedBy=\"CLAIMANT\"";
    private static final String WARRANT_COMPLETED_BY_LEGAL_REP = "warrantCompletedBy=\"LEGAL_REPRESENTATIVE\"";

    private final TextAreaValidationService textAreaValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("statementOfTruth", this::midEvent)
            .pageLabel("Statement of truth")
            .showCondition("selectEnforcementType=\"WARRANT\"")
            .complex(PCSCase::getEnforcementOrder)
                .complex(EnforcementOrder::getWarrantDetails)
                    .label("statementOfTruth-lineSeparator", "---")
                    .complex(WarrantDetails::getStatementOfTruth)
                        .mandatory(StatementOfTruthDetails::getCertification)
                        .label("statementOfTruth-cert-suspended",
                            """
                            <ul class="govuk-list govuk-list--bullet">
                                <li>the defendant has not vacated the land as ordered (*and that the whole or part
                                of any instalments due under the judgment or order have not been paid) ( †and the
                                balance now due is as shown)</li>
                                <li>notice has been given in accordance with The Dwelling Houses (Execution of
                                Possession Orders by Mortgagees) Regulations 2010.</li>
                                <li>a statement of the payments due and made under the judgment or order is attached to
                                this request.††</li>
                            </ul>
                            """,
                            "warrantIsSuspendedOrder=\"YES\""
                        )
                        .label("statementOfTruth-cert-not-suspended",
                            """
                            <ul class="govuk-list govuk-list--bullet">
                                <li>the defendant has not vacated the land as ordered (*and that the whole or part
                                of any instalments due under the judgment or order have not been paid) (†and the
                                balance now due is as shown)</li>
                                <li>notice has been given in accordance with The Dwelling Houses (Execution of
                                Possession Orders by Mortgagees) Regulations 2010.</li>
                            </ul>
                            """,
                            "warrantIsSuspendedOrder=\"NO\""
                        )
                    .done()
                    .complex(WarrantDetails::getRepaymentCosts)
                        .readonly(RepaymentCosts::getStatementOfTruthRepaymentSummaryMarkdown, NEVER_SHOW, true)
                        .label("statementOfTruth-payments-table",
                            "${warrantStatementOfTruthRepaymentSummaryMarkdown}")
                    .done()
                    .complex(WarrantDetails::getStatementOfTruth)
                        .mandatory(StatementOfTruthDetails::getCompletedBy)
                        .mandatory(StatementOfTruthDetails::getAgreementClaimant,
                            WARRANT_COMPLETED_BY_CLAIMANT)
                        .mandatory(StatementOfTruthDetails::getFullNameClaimant,
                            WARRANT_COMPLETED_BY_CLAIMANT)
                        .mandatory(StatementOfTruthDetails::getPositionClaimant,
                            WARRANT_COMPLETED_BY_CLAIMANT)
                        .mandatory(StatementOfTruthDetails::getAgreementLegalRep,
                            WARRANT_COMPLETED_BY_LEGAL_REP)
                        .mandatory(StatementOfTruthDetails::getFullNameLegalRep,
                            WARRANT_COMPLETED_BY_LEGAL_REP)
                        .mandatory(StatementOfTruthDetails::getFirmNameLegalRep,
                            WARRANT_COMPLETED_BY_LEGAL_REP)
                        .mandatory(StatementOfTruthDetails::getPositionLegalRep,
                            WARRANT_COMPLETED_BY_LEGAL_REP)
                    .done()
                .done()
            .done()
            .label("statementOfTruth-saveAndReturn", SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        List<String> errors = new ArrayList<>();

        validateCharacterLimits(caseData, errors);

        return textAreaValidationService.createValidationResponse(caseData, errors);
    }

    private void validateCharacterLimits(PCSCase caseData, List<String> errors) {
        StatementOfTruthDetails statementOfTruth = caseData.getEnforcementOrder()
            .getWarrantDetails()
            .getStatementOfTruth();

        if (statementOfTruth.getCompletedBy() == StatementOfTruthCompletedBy.CLAIMANT) {
            validate(statementOfTruth.getFullNameClaimant(), "Full name", errors);
            validate(statementOfTruth.getPositionClaimant(), "Position or office held", errors);
        } else if (statementOfTruth.getCompletedBy() == StatementOfTruthCompletedBy.LEGAL_REPRESENTATIVE) {
            validate(statementOfTruth.getFullNameLegalRep(), "Full name", errors);
            validate(statementOfTruth.getFirmNameLegalRep(), "Name of firm", errors);
            validate(statementOfTruth.getPositionLegalRep(), "Position or office held", errors);
        }
    }

    private void validate(String value, String label, List<String> errors) {
        textAreaValidationService.validateTextArea(
            value,
            label,
            TextAreaValidationService.STATEMENT_OF_TRUTH_CHARACTER_LIMIT,
            errors
        );
    }

}

