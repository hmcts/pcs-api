package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrantofrestitution;

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
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.PropertyAccessDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrantofrestitution.WarrantOfRestitutionDetails;
import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;
import static uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType.WARRANT_OF_RESTITUTION_FLOW;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;
import uk.gov.hmcts.reform.pcs.ccd.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Component
public class PropertyAccessDetailsWarrantOfRestitutionPage implements CcdPageConfiguration {

    private static final String CLARIFICATION_PROPERTY_ACCESS_LABEL =
            "Explain why it’s difficult to access the property";

    private final TextAreaValidationService textAreaValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("propertyAccessDetailsWarrantOfRestitution", this::midEvent)
                .pageLabel("Access to the property")
                .showCondition(WARRANT_OF_RESTITUTION_FLOW)
                .label("propertyAccessDetailsWarrantOfRestitution-line-separator", "---")
                .complex(PCSCase::getEnforcementOrder)
                .complex(EnforcementOrder::getWarrantOfRestitutionDetails)
                .complex(WarrantOfRestitutionDetails::getPropertyAccessDetails)
                .mandatory(PropertyAccessDetails::getIsDifficultToAccessProperty)
                .mandatory(PropertyAccessDetails::getClarificationOnAccessDifficultyText,
                        "warrant_restIsDifficultToAccessProperty=\"YES\"")
                .done()
                .done()
                .done()
                .label("propertyAccessDetailsWarrantOfRestitution-saveAndReturn", SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> before) {
        PCSCase data = details.getData();
        List<String> errors = getValidationErrors(data);

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(data)
            .errorMessageOverride(StringUtils.joinIfNotEmpty("\n", errors))
            .build();
    }

    private List<String> getValidationErrors(PCSCase data) {
        List<String> errors = new ArrayList<>();

        PropertyAccessDetails propertyAccessDetails =
            data.getEnforcementOrder()
                .getWarrantOfRestitutionDetails()
                .getPropertyAccessDetails();

        if (propertyAccessDetails.getIsDifficultToAccessProperty() == VerticalYesNo.YES) {
            String txt = propertyAccessDetails.getClarificationOnAccessDifficultyText();

            errors.addAll(textAreaValidationService.validateSingleTextArea(
                txt,
                CLARIFICATION_PROPERTY_ACCESS_LABEL,
                TextAreaValidationService.RISK_CATEGORY_EXTRA_LONG_TEXT_LIMIT
            ));
        } else {
            propertyAccessDetails.setClarificationOnAccessDifficultyText(null);
        }
        return errors;
    }
}
