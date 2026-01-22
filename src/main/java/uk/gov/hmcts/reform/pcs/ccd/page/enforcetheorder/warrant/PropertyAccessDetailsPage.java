package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.PropertyAccessDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsWarrantOrWrit;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Component
public class PropertyAccessDetailsPage implements CcdPageConfiguration {

    private static final String CLARIFICATION_PROPERTY_ACCESS_LABEL =
            "Explain why it’s difficult to access the property";

    private final TextAreaValidationService textAreaValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("propertyAccessDetails", this::midEvent)
                .pageLabel("Access to the property")
                .showCondition(ShowConditionsWarrantOrWrit.WARRANT_FLOW)
                .label("propertyAccessDetails-line-separator", "---")
                .complex(PCSCase::getEnforcementOrder)
                .complex(EnforcementOrder::getWarrantDetails)
                .complex(WarrantDetails::getPropertyAccessDetails)
                .mandatory(PropertyAccessDetails::getIsDifficultToAccessProperty)
                .mandatory(PropertyAccessDetails::getClarificationOnAccessDifficultyText,
                        "warrantIsDifficultToAccessProperty=\"YES\"")
                .done()
                .label("propertyAccessDetails-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
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

        String txt = data.getEnforcementOrder()
                .getWarrantDetails().getPropertyAccessDetails().getClarificationOnAccessDifficultyText();

        if (data.getEnforcementOrder().getWarrantDetails().getPropertyAccessDetails().getIsDifficultToAccessProperty()
            .equals(VerticalYesNo.YES)) {
            errors.addAll(textAreaValidationService.validateSingleTextArea(
                txt,
                CLARIFICATION_PROPERTY_ACCESS_LABEL,
                TextAreaValidationService.RISK_CATEGORY_EXTRA_LONG_TEXT_LIMIT
            ));
        }
        return errors;
    }
}
