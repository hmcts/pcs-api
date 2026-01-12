package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.DemotionOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.TextValidationService;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Component
public class DemotionOfTenancyOrderReason implements CcdPageConfiguration {

    private final TextValidationService textValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("demotionOfTenancyOrderReason", this::midEvent)
            .pageLabel("Reasons for requesting a demotion order")
            .showCondition("showDemotionOfTenancyHousingActsPage=\"Yes\"")
            .label("demotionOfTenancyOrderReason-info", "---")
                .complex(PCSCase::getDemotionOfTenancy)
                .mandatory(DemotionOfTenancy::getDemotionOfTenancyReason)
                .done()
            .label("demotionOfTenancyOrderReason-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        List<String> validationErrors = new ArrayList<>();

        DemotionOfTenancy demotionOfTenancy = caseData.getDemotionOfTenancy();
        if (demotionOfTenancy != null) {
            validationErrors.addAll(textValidationService.validateSingleTextArea(
                demotionOfTenancy.getDemotionOfTenancyReason(),
                DemotionOfTenancy.DEMOTION_OF_TENANCY_REASON_LABEL,
                TextValidationService.SHORT_TEXT_LIMIT
            ));
        }

        return textValidationService.createValidationResponse(caseData, validationErrors);
    }
}

