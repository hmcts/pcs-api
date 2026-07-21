package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.WALES;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.and;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.featureFlagsEnabled;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.RELEASE_1_DOT_2;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.WALES_MAKE_A_CLAIM;

@Component
public class DocumentsYouveUploadedChecklistPage implements CcdPageConfiguration {

    private static final String EMPTY_SELECTION_ERROR = "Please select at least one option";

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("documentsYouveUploadedChecklist", this::midEvent)
            .pageLabel("Documents you've uploaded")
            .showCondition(and(WALES, featureFlagsEnabled(RELEASE_1_DOT_2, WALES_MAKE_A_CLAIM)))
            .label("documentsYouveUploadedChecklist-separator", "---")
            .mandatory(PCSCase::getDocumentsYouveUploaded)
            .label("documentsYouveUploadedChecklist-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        if (CollectionUtils.isEmpty(caseData.getDocumentsYouveUploaded())) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .errorMessageOverride(EMPTY_SELECTION_ERROR)
                .build();
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }
}
