package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.PropertyAccessDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.PropertyAccessDetails.CLARIFICATION_PROPERTY_ACCESS_LABEL;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.PropertyAccessDetails.CLARIFICATION_PROPERTY_ACCESS_TEXT_LIMIT;

public class PropertyAccessDetailsPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("propertyAccessDetailsPage", this::midEvent)
                .pageLabel("Access to the property")
                .label("propertyAccessDetailsPage-line-separator", "---")
                .complex(PCSCase::getEnforcementOrder)
                .complex(EnforcementOrder::getPropertyAccessDetails)
                .mandatory(PropertyAccessDetails::getPropertyAccessYesNo)
                .mandatory(PropertyAccessDetails::getClarificationOnAccessDifficultyText,
                        "propertyAccessYesNo=\"YES\"")
                .done()
                .label("violentAggressiveRisk-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> before) {
        PCSCase data = details.getData();
        List<String> errors = new ArrayList<>();

        String txt = data.getEnforcementOrder().getPropertyAccessDetails().getClarificationOnAccessDifficultyText();

        // TODO: Use TextAreaValidationService from PR #751 when merged
        if (txt.length() > CLARIFICATION_PROPERTY_ACCESS_TEXT_LIMIT) {
            errors.add(EnforcementValidationUtil
                    .getCharacterLimitErrorMessage(CLARIFICATION_PROPERTY_ACCESS_LABEL,
                            CLARIFICATION_PROPERTY_ACCESS_TEXT_LIMIT));
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(data)
                .errors(errors)
                .build();
    }
}
