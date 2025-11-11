package uk.gov.hmcts.reform.pcs.ccd.page.makeaclaim;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthAgreementClaimant;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthAgreementLegalRep;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

public class StatementOfTruth implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder.page("statementOfTruth", this::midEvent)
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
            .done();

    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        
        if (caseData.getStatementOfTruth() == null) {
            caseData.setStatementOfTruth(StatementOfTruthDetails.builder().build());
        }
        
        StatementOfTruthDetails statementOfTruth = caseData.getStatementOfTruth();
        StatementOfTruthCompletedBy completedBy = statementOfTruth.getCompletedBy();
        
        // Auto-set the agreement based on who is completing the statement
        if (completedBy == StatementOfTruthCompletedBy.CLAIMANT) {
            statementOfTruth.setAgreementClaimant(java.util.List.of(StatementOfTruthAgreementClaimant.BELIEVE_TRUE));
            // Clear legal rep fields
            statementOfTruth.setAgreementLegalRep(null);
            statementOfTruth.setFullNameLegalRep(null);
            statementOfTruth.setFirmNameLegalRep(null);
            statementOfTruth.setPositionLegalRep(null);
        } else if (completedBy == StatementOfTruthCompletedBy.LEGAL_REPRESENTATIVE) {
            statementOfTruth.setAgreementLegalRep(java.util.List.of(StatementOfTruthAgreementLegalRep.AGREED));
            // Clear claimant fields
            statementOfTruth.setAgreementClaimant(null);
            statementOfTruth.setFullNameClaimant(null);
            statementOfTruth.setPositionClaimant(null);
        }
        
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }

}
