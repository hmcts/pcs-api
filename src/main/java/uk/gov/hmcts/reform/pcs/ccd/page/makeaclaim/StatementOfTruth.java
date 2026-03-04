package uk.gov.hmcts.reform.pcs.ccd.page.makeaclaim;

import static uk.gov.hmcts.ccd.sdk.api.ShowCondition.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthCompletedBy.CLAIMANT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthCompletedBy.LEGAL_REPRESENTATIVE;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.CompletionNextStep;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

public class StatementOfTruth implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder.page("statementOfTruth")
            .pageLabel("Statement of truth")
            .showWhen(when(PCSCase::getCompletionNextStep).is(CompletionNextStep.SUBMIT_AND_PAY_NOW))
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
                .mandatoryWhen(StatementOfTruthDetails::getAgreementClaimant,
                    when(PCSCase::getStatementOfTruth, StatementOfTruthDetails::getCompletedBy).is(CLAIMANT))
                .mandatoryWhen(StatementOfTruthDetails::getFullNameClaimant,
                    when(PCSCase::getStatementOfTruth, StatementOfTruthDetails::getCompletedBy).is(CLAIMANT))
                .mandatoryWhen(StatementOfTruthDetails::getPositionClaimant,
                    when(PCSCase::getStatementOfTruth, StatementOfTruthDetails::getCompletedBy).is(CLAIMANT))
                .mandatoryWhen(StatementOfTruthDetails::getAgreementLegalRep,
                    when(PCSCase::getStatementOfTruth, StatementOfTruthDetails::getCompletedBy)
                        .is(LEGAL_REPRESENTATIVE))
                .mandatoryWhen(StatementOfTruthDetails::getFullNameLegalRep,
                    when(PCSCase::getStatementOfTruth, StatementOfTruthDetails::getCompletedBy)
                        .is(LEGAL_REPRESENTATIVE))
                .mandatoryWhen(StatementOfTruthDetails::getFirmNameLegalRep,
                    when(PCSCase::getStatementOfTruth, StatementOfTruthDetails::getCompletedBy)
                        .is(LEGAL_REPRESENTATIVE))
                .mandatoryWhen(StatementOfTruthDetails::getPositionLegalRep,
                    when(PCSCase::getStatementOfTruth, StatementOfTruthDetails::getCompletedBy)
                        .is(LEGAL_REPRESENTATIVE))
            .done()
            .label("statementOfTruth-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }
}
