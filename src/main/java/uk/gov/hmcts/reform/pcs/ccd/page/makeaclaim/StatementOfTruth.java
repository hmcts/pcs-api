package uk.gov.hmcts.reform.pcs.ccd.page.makeaclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthClaimantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthLegalRepDetails;
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
                .complex(StatementOfTruthDetails::getClaimantDetails,
                    "statementOfTruth.completedBy=\"CLAIMANT\"")
                    .mandatory(StatementOfTruthClaimantDetails::getAgreementClaimant)
                    .mandatory(StatementOfTruthClaimantDetails::getFullNameClaimant)
                    .mandatory(StatementOfTruthClaimantDetails::getPositionClaimant)
                    .done()
                .complex(StatementOfTruthDetails::getLegalRepDetails,
                    "statementOfTruth.completedBy=\"LEGAL_REPRESENTATIVE\"")
                    .mandatory(StatementOfTruthLegalRepDetails::getAgreementLegalRep)
                    .mandatory(StatementOfTruthLegalRepDetails::getFullNameLegalRep)
                    .mandatory(StatementOfTruthLegalRepDetails::getFirmNameLegalRep)
                    .mandatory(StatementOfTruthLegalRepDetails::getPositionLegalRep)
                    .done()
            .done()
            .label("statementOfTruth-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }
}
