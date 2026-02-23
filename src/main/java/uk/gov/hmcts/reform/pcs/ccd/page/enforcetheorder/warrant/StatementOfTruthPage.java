package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RepaymentCosts;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.StatementOfTruthDetailsEnforcement;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsWarrantOrWrit;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

public class StatementOfTruthPage implements CcdPageConfiguration {

    private static final String WARRANT_COMPLETED_BY_CLAIMANT = "warrantCompletedBy=\"CLAIMANT\"";
    private static final String WARRANT_COMPLETED_BY_LEGAL_REP = "warrantCompletedBy=\"LEGAL_REPRESENTATIVE\"";

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("statementOfTruth")
            .pageLabel("Statement of truth")
            .showCondition(ShowConditionsWarrantOrWrit.WARRANT_FLOW)
            .complex(PCSCase::getEnforcementOrder)
                .complex(EnforcementOrder::getWarrantDetails)
                    .label("statementOfTruth-lineSeparator", "---")
                    .complex(WarrantDetails::getStatementOfTruth)
                        .mandatory(StatementOfTruthDetailsEnforcement::getCertification)
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
                        .mandatory(StatementOfTruthDetailsEnforcement::getCompletedBy)
                        .mandatory(StatementOfTruthDetailsEnforcement::getAgreementClaimant,
                            WARRANT_COMPLETED_BY_CLAIMANT)
                        .mandatory(StatementOfTruthDetailsEnforcement::getFullNameClaimant,
                            WARRANT_COMPLETED_BY_CLAIMANT)
                        .mandatory(StatementOfTruthDetailsEnforcement::getPositionClaimant,
                            WARRANT_COMPLETED_BY_CLAIMANT)
                        .mandatory(StatementOfTruthDetailsEnforcement::getAgreementLegalRep,
                            WARRANT_COMPLETED_BY_LEGAL_REP)
                        .mandatory(StatementOfTruthDetailsEnforcement::getFullNameLegalRep,
                            WARRANT_COMPLETED_BY_LEGAL_REP)
                        .mandatory(StatementOfTruthDetailsEnforcement::getFirmNameLegalRep,
                            WARRANT_COMPLETED_BY_LEGAL_REP)
                        .mandatory(StatementOfTruthDetailsEnforcement::getPositionLegalRep,
                            WARRANT_COMPLETED_BY_LEGAL_REP)
                    .done()
                .done()
            .done()
            .label("statementOfTruth-saveAndReturn", SAVE_AND_RETURN);
    }
}

