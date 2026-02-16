package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.DemotionOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Component
public class StatementOfExpressTerms implements CcdPageConfiguration {

    private final TextAreaValidationService textAreaValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("statementOfExpressTerms", this::midEvent)
            .pageLabel("Statement of express terms")
            .showCondition("demotionOfTenancy_ShowHousingActsPage=\"Yes\""
                               + " OR suspensionToBuyDemotionOfTenancyPages=\"Yes\"")
            .label("statementOfExpressTerms-info", "---")
                .complex(PCSCase::getDemotionOfTenancy)
                .mandatory(DemotionOfTenancy::getStatementOfExpressTermsServed)
                .mandatory(DemotionOfTenancy::getStatementOfExpressTermsDetails,
                           "demotionOfTenancy_StatementOfExpressTermsServed=\"YES\"")
                .done()
            .label("statementOfExpressTerms-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        List<String> validationErrors = new ArrayList<>();

        DemotionOfTenancy demotionOfTenancy = caseData.getDemotionOfTenancy();
        if (demotionOfTenancy != null) {
            validationErrors.addAll(textAreaValidationService.validateSingleTextArea(
                demotionOfTenancy.getStatementOfExpressTermsDetails(),
                DemotionOfTenancy.STATEMENT_OF_EXPRESS_TERMS_DETAILS_LABEL,
                TextAreaValidationService.LONG_TEXT_LIMIT
            ));
        }

        return textAreaValidationService.createValidationResponse(caseData, validationErrors);
    }
}
