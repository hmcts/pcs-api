package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.PropertyAccessDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

import java.util.ArrayList;
import java.util.List;

public class PropertyAccessDetailsPage implements CcdPageConfiguration {

    private static final String CLARIFICATION_PROPERTY_ACCESS_LABEL =
            "Explain why it's difficult to access the property";
    private static final int CLARIFICATION_PROPERTY_ACCESS_TEXT_LIMIT = 6800;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("propertyAccessDetails", this::midEvent)
                .pageLabel("Access to the property")
                .label("propertyAccessDetails-line-separator", "---")
                .complex(PCSCase::getEnforcementOrder)
                .complex(EnforcementOrder::getPropertyAccessDetails)
                .mandatory(PropertyAccessDetails::getIsDifficultToAccessProperty)
                .mandatory(PropertyAccessDetails::getClarificationOnAccessDifficultyText,
                        "isDifficultToAccessProperty=\"YES\"")
                .done()
                .label("propertyAccessDetails-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> before) {
        PCSCase data = details.getData();
        List<String> errors = new ArrayList<>();

        // Use TextAreaValidationService from PR #751 when merged
        String txt = data.getEnforcementOrder().getPropertyAccessDetails().getClarificationOnAccessDifficultyText();
        if (data.getEnforcementOrder().getPropertyAccessDetails().getIsDifficultToAccessProperty()
            .equals(VerticalYesNo.YES) && (txt.length() > CLARIFICATION_PROPERTY_ACCESS_TEXT_LIMIT)) {
            errors.add(EnforcementValidationUtil.getCharacterLimitErrorMessage(CLARIFICATION_PROPERTY_ACCESS_LABEL,
                CLARIFICATION_PROPERTY_ACCESS_TEXT_LIMIT));
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(data)
                .errors(errors)
                .build();
    }
}
