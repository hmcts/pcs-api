package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RepaymentCosts;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.StatementOfTruthDetailsEnforcement;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsWarrantOrWrit;

public class StatementOfTruthWritPage implements CcdPageConfiguration {

    private static final String WRIT_COMPLETED_BY_CLAIMANT = "writCompletedBy=\"CLAIMANT\"";
    private static final String WRIT_COMPLETED_BY_LEGAL_REPRESENTATIVE = "writCompletedBy=\"LEGAL_REPRESENTATIVE\"";

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("statementOfTruthWrit")
            .pageLabel("Statement of truth")
            .showCondition(ShowConditionsWarrantOrWrit.WRIT_FLOW)
            .label("statementOfTruthWrit-line-separator", "---")
            .label("statementOfTruthWrit-declaration",
                   """
                  <p class="govuk-body">
                  I certify that the details I have given are correct and that to my knowledge there is no application
                  or other procedure pending.</p>
                  <p class="govuk-body">
                  I request an order for enforcement in the High Court by writ of possession.</p>
                  """
            )
            .complex(PCSCase:: getEnforcementOrder)
            .complex(EnforcementOrder::getWritDetails)
            .complex(WritDetails::getRepaymentCosts)
            .readonly(RepaymentCosts::getStatementOfTruthRepaymentSummaryMarkdown, NEVER_SHOW, true)
            .label("statementOfTruthWrit-payments-table",
                   "${writStatementOfTruthRepaymentSummaryMarkdown}")
            .done()
            .complex(WritDetails::getStatementOfTruth)
            .mandatory(StatementOfTruthDetailsEnforcement::getCompletedBy)
            .mandatory(StatementOfTruthDetailsEnforcement::getAgreementClaimant,
                       WRIT_COMPLETED_BY_CLAIMANT
            )
            .mandatory(StatementOfTruthDetailsEnforcement::getFullNameClaimant,
                       WRIT_COMPLETED_BY_CLAIMANT
            )
            .mandatory(StatementOfTruthDetailsEnforcement::getPositionClaimant,
                       WRIT_COMPLETED_BY_CLAIMANT
            )
            .mandatory(StatementOfTruthDetailsEnforcement::getAgreementLegalRep,
                       WRIT_COMPLETED_BY_LEGAL_REPRESENTATIVE
            )
            .mandatory(StatementOfTruthDetailsEnforcement::getFullNameLegalRep,
                       WRIT_COMPLETED_BY_LEGAL_REPRESENTATIVE
            )
            .mandatory(StatementOfTruthDetailsEnforcement::getFirmNameLegalRep,
                       WRIT_COMPLETED_BY_LEGAL_REPRESENTATIVE
            )
            .mandatory(StatementOfTruthDetailsEnforcement::getPositionLegalRep,
                       WRIT_COMPLETED_BY_LEGAL_REPRESENTATIVE
            )
            .done()
            .done()
            .done()
            .label("statementOfTruthWrit-saveAndReturn", SAVE_AND_RETURN);
    }
}
