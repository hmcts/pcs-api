package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RepaymentCosts;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.StatementOfTruthDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsWarrantOrWrit;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.ArrayList;
import java.util.List;


@AllArgsConstructor
@Component
public class StatementOfTruthWritPage implements CcdPageConfiguration {

    private static final String WRIT_COMPLETED_BY_CLAIMANT = "writCompletedBy=\"CLAIMANT\"";
    private static final String WRIT_COMPLETED_BY_LEGAL_REPRESENTATIVE = "writCompletedBy=\"LEGAL_REPRESENTATIVE\"";

    private final TextAreaValidationService textAreaValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("statementOfTruthWrit", this::midEvent)
            .pageLabel("Statement of truth")
            .showCondition(ShowConditionsWarrantOrWrit.WRIT_FLOW)
            .label("statementOfTruth-writ-line-separator", "---")
            .label("statementOfTruth-writ-declaration",
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
            .label("statementOfTruth-payments-table",
                   "${writStatementOfTruthRepaymentSummaryMarkdown}")
            .done()
            .complex(WritDetails::getStatementOfTruth)
            .mandatory(StatementOfTruthDetails::getCompletedBy)
            .mandatory(StatementOfTruthDetails::getAgreementClaimant,
                       WRIT_COMPLETED_BY_CLAIMANT
            )
            .mandatory(StatementOfTruthDetails::getFullNameClaimant,
                       WRIT_COMPLETED_BY_CLAIMANT
            )
            .mandatory(StatementOfTruthDetails::getPositionClaimant,
                       WRIT_COMPLETED_BY_CLAIMANT
            )
            .mandatory(StatementOfTruthDetails::getAgreementLegalRep,
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
            .label("statementOfTruthWritPlaceholder-saveAndReturn", SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        List<String> errors = new ArrayList<>();

        validateCharacterLimits(caseData, errors);

        return textAreaValidationService.createValidationResponse(caseData, errors);
    }

    private void validateCharacterLimits(PCSCase caseData, List<String> errors) {
        StatementOfTruthDetails statementOfTruth = caseData.getEnforcementOrder()
            .getWarrantDetails()
            .getStatementOfTruth();

        if (statementOfTruth.getCompletedBy() == StatementOfTruthCompletedBy.CLAIMANT) {
            validate(statementOfTruth.getFullNameClaimant(), "Full name", errors);
            validate(statementOfTruth.getPositionClaimant(), "Position or office held", errors);
        } else if (statementOfTruth.getCompletedBy() == StatementOfTruthCompletedBy.LEGAL_REPRESENTATIVE) {
            validate(statementOfTruth.getFullNameLegalRep(), "Full name", errors);
            validate(statementOfTruth.getFirmNameLegalRep(), "Name of firm", errors);
            validate(statementOfTruth.getPositionLegalRep(), "Position or office held", errors);
        }
    }

    private void validate(String value, String label, List<String> errors) {
        textAreaValidationService.validateTextArea(
            value,
            label,
            TextAreaValidationService.EXTRA_SHORT_TEXT_LIMIT,
            errors
        );
    }
}
