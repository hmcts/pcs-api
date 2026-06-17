package uk.gov.hmcts.reform.pcs.ccd.page.makeaclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

@Component
@AllArgsConstructor
public class StatementOfTruth implements CcdPageConfiguration {

    private final PcsCaseService pcsCaseService;

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
                .mandatory(StatementOfTruthDetails::getFullNameParty,
                    "statementOfTruth.completedBy=\"CLAIMANT\"")
                .mandatory(StatementOfTruthDetails::getPositionParty,
                    "statementOfTruth.completedBy=\"CLAIMANT\"")
                .mandatory(StatementOfTruthDetails::getAgreementClaimantLegalRep,
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

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {

        pcsCaseService.allocateRegionId(details.getData());
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(details.getData())
            .build();
    }

}
