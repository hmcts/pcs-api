package uk.gov.hmcts.reform.pcs.ccd.page.makeaclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

public class StatementOfTruth implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder.page("statementOfTruth")
            .pageLabel("Statement of truth")
            .showCondition("completionNextStep=\"SUBMIT_AND_PAY_NOW\"")
            .label("statementOfTruth-body",
                """
                ---
                <p class="govuk-body">
                  I understand that proceedings for contempt of court may be brought against
                  anyone who makes, or causes to be made, a false statement in a document
                  verified by a statement of truth without an honest belief in its truth.
                </p>
                """
            )
            .complex(PCSCase::getStatementOfTruth)
                .mandatory(StatementOfTruthDetails::getCompletedBy)
                .mandatory(StatementOfTruthDetails::getAgreementClaimant,
                    "statementOfTruth.completedBy=\"CLAIMANT\"")
                .mandatory(StatementOfTruthDetails::getFullNameClaimant,
                    "statementOfTruth.completedBy=\"CLAIMANT\"")
                .mandatory(StatementOfTruthDetails::getPositionClaimant,
                    "statementOfTruth.completedBy=\"CLAIMANT\"")
                .mandatory(StatementOfTruthDetails::getAgreementLegalRep,
                    "statementOfTruth.completedBy=\"LEGAL_REPRESENTATIVE\"")
                .mandatory(StatementOfTruthDetails::getFullNameLegalRep,
                    "statementOfTruth.completedBy=\"LEGAL_REPRESENTATIVE\"")
                .mandatory(StatementOfTruthDetails::getFirmNameLegalRep,
                    "statementOfTruth.completedBy=\"LEGAL_REPRESENTATIVE\"")
                .mandatory(StatementOfTruthDetails::getPositionLegalRep,
                    "statementOfTruth.completedBy=\"LEGAL_REPRESENTATIVE\"")
            .done()
            .label("statementOfTruth-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }
}
