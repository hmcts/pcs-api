package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.AdditionalInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.service.TextValidationService;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.AdditionalInformation.ADDITIONAL_INFORMATION_DETAILS_LABEL;
import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

@AllArgsConstructor
@Component
public class AdditionalInformationPage implements CcdPageConfiguration {

    private final TextValidationService textValidationService;
    private static final String SHOW_CONDITION = "additionalInformationSelect=\"YES\"";

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("additionalInformationPage", this::midEvent)
            .pageLabel("Anything else that could help with the eviction ")
            .label("additionalInformationPage-separator", "---")
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getAdditionalInformation)
            .mandatoryWithLabel(
                AdditionalInformation::getAdditionalInformationSelect,
                "Do you want to tell us anything else that could help with the eviction?"
            )
            .mandatory(AdditionalInformation::getAdditionalInformationDetails, SHOW_CONDITION)
            .done()
            .label("additionalInformationPage-details-save-and-return", SAVE_AND_RETURN);

    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> before) {
        PCSCase data = details.getData();
        List<String> errors = getValidationErrors(data);
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(data)
            .errors(errors)
            .build();
    }

    private List<String> getValidationErrors(PCSCase data) {
        List<String> errors = new ArrayList<>();

        AdditionalInformation additionalInformation = data.getEnforcementOrder().getAdditionalInformation();
        if (additionalInformation.getAdditionalInformationSelect().toBoolean()) {
            String txt = data.getEnforcementOrder().getAdditionalInformation().getAdditionalInformationDetails();
            errors.addAll(textValidationService.validateSingleTextArea(
                txt,
                ADDITIONAL_INFORMATION_DETAILS_LABEL,
                TextValidationService.RISK_CATEGORY_EXTRA_LONG_TEXT_LIMIT
            ));
        }
        return errors;
    }
}
