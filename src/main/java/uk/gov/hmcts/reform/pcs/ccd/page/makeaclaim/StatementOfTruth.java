package uk.gov.hmcts.reform.pcs.ccd.page.makeaclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

public class StatementOfTruth implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder.page("statementOfTruth")
            .pageLabel("Statement of truth")
            .showCondition("completionNextStep=\"SUBMIT_AND_PAY_NOW\"")
            .label("statementOfTruth-hr", "---")
            .label("statementOfTruth-body",
                """
                <p class="govuk-body">
                  I understand that proceedings for contempt of court may be brought against
                  anyone who makes, or causes to be made, a false statement in a document
                  verified by a statement of truth without an honest belief in its truth.
                </p>
                """
            )
            .mandatory(PCSCase::getStatementOfTruthCompletedBy)
            .mandatory(PCSCase::getStatementOfTruthAgreementClaimant,
                "statementOfTruthCompletedBy=\"CLAIMANT\"")
            .mandatory(PCSCase::getStatementOfTruthFullNameClaimant,
                "statementOfTruthCompletedBy=\"CLAIMANT\"")
            .mandatory(PCSCase::getStatementOfTruthPositionClaimant,
                "statementOfTruthCompletedBy=\"CLAIMANT\"")
            .mandatory(PCSCase::getStatementOfTruthAgreementLegalRep,
                "statementOfTruthCompletedBy=\"LEGAL_REPRESENTATIVE\"")
            .mandatory(PCSCase::getStatementOfTruthFullNameLegalRep,
                "statementOfTruthCompletedBy=\"LEGAL_REPRESENTATIVE\"")
            .mandatory(PCSCase::getStatementOfTruthFirmNameLegalRep,
                "statementOfTruthCompletedBy=\"LEGAL_REPRESENTATIVE\"")
            .mandatory(PCSCase::getStatementOfTruthPositionLegalRep,
                "statementOfTruthCompletedBy=\"LEGAL_REPRESENTATIVE\"");

    }


}
