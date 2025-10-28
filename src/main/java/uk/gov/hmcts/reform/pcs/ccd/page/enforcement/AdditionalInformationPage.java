package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.AdditionalInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

public class AdditionalInformationPage implements CcdPageConfiguration {

    public static final String DETAILS_TOO_LONG_ERROR_MSG = "Please limit the details to 6800 characters or less.";
    public static final String NO_DETAILS_ERROR_MSG = "Please provide details for the additional information.";
    private static final String ADDITIONAL_INFORMATION_INFO = "additionalInformation-Info";
    private static final String SHOW_CONDITION = "additionalInformationSelect=\"YES\"";

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("additionalInformationPage", this::midEvent)
            .pageLabel("Anything else that could help with the eviction ")
            .complex(PCSCase::getEnforcementOrder)
            .label(
                ADDITIONAL_INFORMATION_INFO, """
                    ---
                    <h2>Do you want to tell us anything else that could help with the eviction?</h2>
                    """)
            .complex(EnforcementOrder::getAdditionalInformation)
                .mandatory(AdditionalInformation::getAdditionalInformationSelect)
                    .mandatory(AdditionalInformation::getAdditionalInformationDetails, SHOW_CONDITION)
            .done()
            .label("additionalInformationPage-details-save-and-return", SAVE_AND_RETURN);

    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> before) {
        PCSCase data = details.getData();
        List<String> errors = new ArrayList<>();
        AdditionalInformation additionalInformation = data.getEnforcementOrder().getAdditionalInformation();
        if (additionalInformation.getAdditionalInformationSelect().toBoolean()) {
            String txt = data.getEnforcementOrder().getAdditionalInformation().getAdditionalInformationDetails();
            if (StringUtils.isBlank(txt) || txt.trim().isEmpty() || txt.matches("\\s+")) {
                errors.add(NO_DETAILS_ERROR_MSG);
            } else {
                // Refactor validation logic to use TextAreaValidationService from PR #751 when merged
                if (txt.length() > 6800) {
                    // Use TextAreaValidationService from PR #751 when merged
                    errors.add(DETAILS_TOO_LONG_ERROR_MSG);
                }
            }
        } else {
            additionalInformation.setAdditionalInformationDetails(null);
        }
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(data)
            .errors(errors)
            .build();
    }

}
