package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrantofrestitution;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrantofrestitution.WarrantOfRestitutionDetails;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;
import static uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType.WARRANT_OF_RESTITUTION_FLOW;

@AllArgsConstructor
@Component
public class ExplainHowDefendantsReturnedPage implements CcdPageConfiguration {

    private static final String FIELD_LABEL =
            "How did the defendants return to the property?";

    private final TextAreaValidationService textAreaValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("explainHowDefendantsReturned", this::midEvent)
            .pageLabel("Explain how the defendants returned to the property after the eviction")
            .showCondition(WARRANT_OF_RESTITUTION_FLOW)
            .label("explainHowDefendantsReturned-line-separator", "---")
            .label(
                "explainHowDefendantsReturned-examples",
                """
                <p class="govuk-body govuk-!-margin-bottom-1">
                For example, explain if you have a:
                </p>
                <ul class="govuk-list govuk-list--bullet">
                    <li class="govuk-!-font-size-19">police report about the defendants breaking into the property,
                    and include the crime reference number if you have one</li>
                    <li class="govuk-!-font-size-19">witness statement, for example from a neighbour who saw them
                    return to the property</li>
                    <li class="govuk-!-font-size-19">photograph of damage to the property caused by the defendants
                    when they returned, for example a broken window or door</li>
                </ul>
                <p class="govuk-body govuk-!-margin-bottom-1">
                If you can, include:
                </p>
                <ul class="govuk-list govuk-list--bullet">
                    <li class="govuk-!-font-size-19">when they returned, for example the date</li>
                    <li class="govuk-!-font-size-19">the names of the defendants who returned</li>
                    <li class="govuk-!-font-size-19">any other evidence you have that proves that the defendants are
                    currently living at the property</li>
                </ul>
                """
            )
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getWarrantOfRestitutionDetails)
            .mandatory(WarrantOfRestitutionDetails::getHowDefendantsReturned)
            .done()
            .done()
            .label("explainHowDefendantsReturned-save-and-return", SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> before) {
        PCSCase caseData = details.getData();
        String value = caseData.getEnforcementOrder()
            .getWarrantOfRestitutionDetails()
            .getHowDefendantsReturned();
        List<String> errors = textAreaValidationService.validateSingleTextArea(
            value,
            FIELD_LABEL,
            TextAreaValidationService.RISK_CATEGORY_EXTRA_LONG_TEXT_LIMIT
        );
        return textAreaValidationService.createValidationResponse(caseData, errors);
    }
}
