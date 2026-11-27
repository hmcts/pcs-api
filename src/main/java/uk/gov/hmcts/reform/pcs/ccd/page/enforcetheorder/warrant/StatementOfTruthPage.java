package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RepaymentCosts;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RawWarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

public class StatementOfTruthPage implements CcdPageConfiguration {

    private static final String WARRANT_COMPLETED_BY_CLAIMANT =
            "warrantStatementOfTruth.completedBy=\"CLAIMANT\"";
    private static final String WARRANT_COMPLETED_BY_LEGAL_REP =
            "warrantStatementOfTruth.completedBy=\"LEGAL_REPRESENTATIVE\"";

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("statementOfTruth")
            .pageLabel("Statement of truth")
            .showCondition(ShowConditionsEnforcementType.WARRANT_FLOW)
            .complex(PCSCase::getEnforcementOrder)
                .complex(EnforcementOrder::getWarrantDetails)
                    .label("statementOfTruth-lineSeparator", "---")
                    .mandatoryNoSummary(WarrantDetails::getCertification)
                    .label("statementOfTruth-cert-suspended",
                        """
                        <div class="govuk-!-margin-left-6 govuk-!-margin-top-0">
                        <ul class="govuk-list govuk-list--bullet govuk-!-margin-top-0">
                            <li>the defendant has not vacated the land as ordered and has failed to
                            comply with the terms upon which the order was suspended.</li>
                            <li>a statement of the payments due and made under the judgement or order
                            is attached to this request.</li>
                            <li>I believe that the facts stated in this form are true</li>
                        </ul>
                        </div>
                        """,
                        "warrantIsSuspendedOrder=\"YES\""
                    )
                    .label("statementOfTruth-cert-not-suspended",
                        """
                        <div class="govuk-!-margin-left-6 govuk-!-margin-top-0">
                        <ul class="govuk-list govuk-list--bullet" style="margin-left: 40px;">
                            <li>the defendant has not vacated the land as ordered</li>
                            <li>I believe the facts stated in this form are true.</li>
                        </ul>
                        </div>
                        """,
                        "warrantIsSuspendedOrder=\"NO\""
                    )
                    .complex(WarrantDetails::getRepaymentCosts)
                        .readonly(RepaymentCosts::getStatementOfTruthRepaymentSummaryMarkdown, NEVER_SHOW, true)
                        .label("statementOfTruth-payments-table",
                            "${warrantStatementOfTruthRepaymentSummaryMarkdown}")
                    .done()
                .done()
                .complex(EnforcementOrder::getRawWarrantDetails)
                    .complex(RawWarrantDetails::getWarrantStatementOfTruth)
                        .mandatory(StatementOfTruthDetails::getCompletedBy)
                        .mandatory(StatementOfTruthDetails::getAgreementClaimant,
                            WARRANT_COMPLETED_BY_CLAIMANT)
                        .mandatory(StatementOfTruthDetails::getFullNameParty,
                            WARRANT_COMPLETED_BY_CLAIMANT)
                        .mandatory(StatementOfTruthDetails::getPositionParty,
                            WARRANT_COMPLETED_BY_CLAIMANT)
                        .mandatory(StatementOfTruthDetails::getAgreementClaimantLegalRep,
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
}

