package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthClaimantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthLegalRepDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RepaymentCosts;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

@AllArgsConstructor
@Component
public class EnforcementStatementOfTruthPage implements CcdPageConfiguration {

    private static final int CHARACTER_LIMIT = 60;

    private final TextAreaValidationService textAreaValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("enforcementStatementOfTruth", this::midEvent)
            .pageLabel("Statement of truth")
            .showCondition("selectEnforcementType=\"WARRANT\"")
            .readonly(PCSCase::getFormattedPropertyAddress, NEVER_SHOW)
            .complex(PCSCase::getEnforcementOrder)
                .complex(EnforcementOrder::getWarrantDetails)
                    .label("enforcementStatementOfTruth-lineSeparator", "---")
                    .complex(WarrantDetails::getRepaymentCosts)
                        .readonly(RepaymentCosts::getStatementOfTruthRepaymentSummaryMarkdown, NEVER_SHOW, true)
                    .done()
                    .complex(WarrantDetails::getStatementOfTruth)
                        .mandatory(StatementOfTruthDetails::getEnforcementCertification)
                        .label("enforcementSOT-cert-suspended",
                            """
                            <ul class="govuk-list govuk-list--bullet">
                            <li>the defendant has not vacated the land as ordered (* and that the whole or part of any 
                            instalments due under the judgment or order have not been paid) (and the balance now due is as 
                            shown)</li>
                            <li>notice has been given in accordance with The Dwelling Houses (Execution of Possession Order by 
                            Mortgagees) Regulations 2010.</li>
                            <li>a statement of the payments due and made under the judgment or order is attached to this 
                            request</li>
                            </ul>
                            """,
                            "isSuspendedOrder=\"YES\""
                        )
                        .label("enforcementSOT-cert-not-suspended",
                            """
                            <ul class="govuk-list govuk-list--bullet">
                            <li>the defendant has not vacated the land as ordered (* and that the whole or part of any 
                            instalments due under the judgment or order have not been paid) (and the balance now due is as 
                            shown)</li>
                            <li>notice has been given in accordance with The Dwelling Houses (Execution of Possession Order by 
                            Mortgagees) Regulations 2010.</li>
                            </ul>
                            """,
                            "isSuspendedOrder=\"NO\""
                        )
                        .label("enforcementSOT-payments-table",
                            "${repaymentStatementOfTruthRepaymentSummaryMarkdown}")
                        .mandatory(StatementOfTruthDetails::getCompletedBy)
                        .complex(StatementOfTruthDetails::getClaimantDetails,
                            "warrantCompletedBy=\"CLAIMANT\"")
                            .mandatory(StatementOfTruthClaimantDetails::getAgreementClaimant)
                            .mandatory(StatementOfTruthClaimantDetails::getFullNameClaimant)
                            .mandatory(StatementOfTruthClaimantDetails::getPositionClaimant)
                        .done()
                        .complex(StatementOfTruthDetails::getLegalRepDetails,
                            "warrantCompletedBy=\"LEGAL_REPRESENTATIVE\"")
                            .mandatory(StatementOfTruthLegalRepDetails::getAgreementLegalRep)
                            .mandatory(StatementOfTruthLegalRepDetails::getFullNameLegalRep)
                            .mandatory(StatementOfTruthLegalRepDetails::getFirmNameLegalRep)
                            .mandatory(StatementOfTruthLegalRepDetails::getPositionLegalRep)
                        .done()
                    .done()
                .done()
            .done()
            .label("enforcementStatementOfTruth-saveAndReturn", SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {

        PCSCase caseData = details.getData();
        List<String> errors = new ArrayList<>();

        // Validate character limits only - CCD's .mandatory() handles checkbox validation
        validateCharacterLimits(caseData, errors);

        return textAreaValidationService.createValidationResponse(caseData, errors);
    }

    private void validateCharacterLimits(PCSCase caseData, List<String> errors) {
        StatementOfTruthDetails statementOfTruth = caseData.getEnforcementOrder().getWarrantDetails()
                .getStatementOfTruth();

        if (statementOfTruth == null) {
            return;
        }

        if (statementOfTruth.getCompletedBy() != null) {
            if (statementOfTruth.getCompletedBy().name().equals("CLAIMANT")) {
                StatementOfTruthClaimantDetails claimantDetails = statementOfTruth.getClaimantDetails();
                if (claimantDetails != null) {
                    textAreaValidationService.validateTextArea(
                        claimantDetails.getFullNameClaimant(),
                        "Full name",
                        CHARACTER_LIMIT,
                        errors
                    );
                    textAreaValidationService.validateTextArea(
                        claimantDetails.getPositionClaimant(),
                        "Position or office held",
                        CHARACTER_LIMIT,
                        errors
                    );
                }
            } else if (statementOfTruth.getCompletedBy().name().equals("LEGAL_REPRESENTATIVE")) {
                StatementOfTruthLegalRepDetails legalRepDetails = statementOfTruth.getLegalRepDetails();
                if (legalRepDetails != null) {
                    textAreaValidationService.validateTextArea(
                        legalRepDetails.getFullNameLegalRep(),
                        "Full name",
                        CHARACTER_LIMIT,
                        errors
                    );
                    textAreaValidationService.validateTextArea(
                        legalRepDetails.getFirmNameLegalRep(),
                        "Name of firm",
                        CHARACTER_LIMIT,
                        errors
                    );
                    textAreaValidationService.validateTextArea(
                        legalRepDetails.getPositionLegalRep(),
                        "Position or office held",
                        CHARACTER_LIMIT,
                        errors
                    );
                }
            }
        }
    }
}

