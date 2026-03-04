package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RepaymentCosts;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.StatementOfTruthDetailsEnforcement;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType;

import static uk.gov.hmcts.ccd.sdk.api.ShowCondition.when;
import static uk.gov.hmcts.ccd.sdk.api.ShowCondition.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo.NO;
import static uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo.YES;
import static uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthCompletedBy.CLAIMANT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthCompletedBy.LEGAL_REPRESENTATIVE;
import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

public class StatementOfTruthPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("statementOfTruth")
            .pageLabel("Statement of truth")
            .showWhen(ShowConditionsEnforcementType.WARRANT_FLOW)
            .complex(PCSCase::getEnforcementOrder)
                .complex(EnforcementOrder::getWarrantDetails)
                    .label("statementOfTruth-lineSeparator", "---")
                    .complex(WarrantDetails::getStatementOfTruth)
                        .mandatory(StatementOfTruthDetailsEnforcement::getCertification)
                        .labelWhen("statementOfTruth-cert-suspended",
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
                            when(EnforcementOrder::getWarrantDetails, WarrantDetails::getIsSuspendedOrder).is(YES))
                        .labelWhen("statementOfTruth-cert-not-suspended",
                            """
                            <ul class="govuk-list govuk-list--bullet">
                                <li>the defendant has not vacated the land as ordered (*and that the whole or part
                                of any instalments due under the judgment or order have not been paid) (†and the
                                balance now due is as shown)</li>
                                <li>notice has been given in accordance with The Dwelling Houses (Execution of
                                Possession Orders by Mortgagees) Regulations 2010.</li>
                            </ul>
                            """,
                            when(EnforcementOrder::getWarrantDetails, WarrantDetails::getIsSuspendedOrder).is(NO))
                    .done()
                    .complex(WarrantDetails::getRepaymentCosts)
                        .readonly(RepaymentCosts::getStatementOfTruthRepaymentSummaryMarkdown, NEVER_SHOW, true)
                        .label("statementOfTruth-payments-table",
                            "${warrantStatementOfTruthRepaymentSummaryMarkdown}")
                    .done()
                    .complex(WarrantDetails::getStatementOfTruth)
                        .mandatory(StatementOfTruthDetailsEnforcement::getCompletedBy)
                        .mandatoryWhen(StatementOfTruthDetailsEnforcement::getAgreementClaimant,
                            when(EnforcementOrder::getWarrantDetails, WarrantDetails::getStatementOfTruth,
                                StatementOfTruthDetailsEnforcement::getCompletedBy).is(CLAIMANT))
                        .mandatoryWhen(StatementOfTruthDetailsEnforcement::getFullNameClaimant,
                            when(EnforcementOrder::getWarrantDetails, WarrantDetails::getStatementOfTruth,
                                StatementOfTruthDetailsEnforcement::getCompletedBy).is(CLAIMANT))
                        .mandatoryWhen(StatementOfTruthDetailsEnforcement::getPositionClaimant,
                            when(EnforcementOrder::getWarrantDetails, WarrantDetails::getStatementOfTruth,
                                StatementOfTruthDetailsEnforcement::getCompletedBy).is(CLAIMANT))
                        .mandatoryWhen(StatementOfTruthDetailsEnforcement::getAgreementLegalRep,
                            when(EnforcementOrder::getWarrantDetails, WarrantDetails::getStatementOfTruth,
                                StatementOfTruthDetailsEnforcement::getCompletedBy).is(LEGAL_REPRESENTATIVE))
                        .mandatoryWhen(StatementOfTruthDetailsEnforcement::getFullNameLegalRep,
                            when(EnforcementOrder::getWarrantDetails, WarrantDetails::getStatementOfTruth,
                                StatementOfTruthDetailsEnforcement::getCompletedBy).is(LEGAL_REPRESENTATIVE))
                        .mandatoryWhen(StatementOfTruthDetailsEnforcement::getFirmNameLegalRep,
                            when(EnforcementOrder::getWarrantDetails, WarrantDetails::getStatementOfTruth,
                                StatementOfTruthDetailsEnforcement::getCompletedBy).is(LEGAL_REPRESENTATIVE))
                        .mandatoryWhen(StatementOfTruthDetailsEnforcement::getPositionLegalRep,
                            when(EnforcementOrder::getWarrantDetails, WarrantDetails::getStatementOfTruth,
                                StatementOfTruthDetailsEnforcement::getCompletedBy).is(LEGAL_REPRESENTATIVE))
                    .done()
                .done()
            .done()
            .label("statementOfTruth-saveAndReturn", SAVE_AND_RETURN);
    }
}
