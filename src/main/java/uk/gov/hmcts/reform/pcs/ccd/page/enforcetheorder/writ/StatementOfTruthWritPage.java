package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RepaymentCosts;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.RawWritDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType;

public class StatementOfTruthWritPage implements CcdPageConfiguration {

    private static final String WRIT_COMPLETED_BY_CLAIMANT = "writStatementOfTruth.completedBy=\"CLAIMANT\"";
    private static final String WRIT_COMPLETED_BY_LEGAL_REPRESENTATIVE =
            "writStatementOfTruth.completedBy=\"LEGAL_REPRESENTATIVE\"";

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("statementOfTruthWrit")
            .pageLabel("Statement of truth")
            .showCondition(ShowConditionsEnforcementType.WRIT_FLOW)
            .complex(PCSCase::getEnforcementOrder)
                .complex(EnforcementOrder::getWritDetails)
                    .label("statementOfTruthWrit-line-separator", "---")
                .mandatoryNoSummary(WritDetails::getCertification)
                    .label("statementOfTruthWrit-declaration",
                           """
                           <p class="govuk-body">
                           the details I have given are correct and that to my knowledge there is no application
                           or other procedure pending.</p>
                           <p class="govuk-body">
                           I request an order for enforcement in the High Court by writ of possession.</p>
                           <p class="govuk-body">
                           I believe that the facts stated in this form are true.</p>
                           """
                    )
                    .complex(WritDetails::getRepaymentCosts)
                        .readonly(RepaymentCosts::getStatementOfTruthRepaymentSummaryMarkdown, NEVER_SHOW, true)
                        .label("statementOfTruthWrit-payments-table",
                               "${writStatementOfTruthRepaymentSummaryMarkdown}")
                    .done()
                .done()
                .complex(EnforcementOrder::getRawWritDetails)
                    .complex(RawWritDetails::getWritStatementOfTruth)
                        .mandatory(StatementOfTruthDetails::getCompletedBy)
                        .mandatory(StatementOfTruthDetails::getAgreementClaimant,
                                WRIT_COMPLETED_BY_CLAIMANT
                        )
                        .mandatory(StatementOfTruthDetails::getFullNameParty,
                               WRIT_COMPLETED_BY_CLAIMANT
                        )
                        .mandatory(StatementOfTruthDetails::getPositionParty,
                               WRIT_COMPLETED_BY_CLAIMANT
                        )
                        .mandatory(StatementOfTruthDetails::getAgreementClaimantLegalRep,
                               WRIT_COMPLETED_BY_LEGAL_REPRESENTATIVE
                        )
                        .mandatory(StatementOfTruthDetails::getFullNameLegalRep,
                               WRIT_COMPLETED_BY_LEGAL_REPRESENTATIVE
                        )
                        .mandatory(StatementOfTruthDetails::getFirmNameLegalRep,
                               WRIT_COMPLETED_BY_LEGAL_REPRESENTATIVE
                        )
                        .mandatory(StatementOfTruthDetails::getPositionLegalRep,
                               WRIT_COMPLETED_BY_LEGAL_REPRESENTATIVE
                        )
                    .done()
                .done()
            .done()
            .label("statementOfTruthWrit-saveAndReturn", SAVE_AND_RETURN);
    }
}
