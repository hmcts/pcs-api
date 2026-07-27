package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.WALES;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.and;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.featureFlagsEnabled;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.RELEASE_1_DOT_2;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.WALES_MAKE_A_CLAIM;

@Component
public class DocumentsYouveUploadedChecklistPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("documentsYouveUploadedChecklist")
            .pageLabel("Documents you've uploaded")
            .showCondition(and(WALES, featureFlagsEnabled(RELEASE_1_DOT_2, WALES_MAKE_A_CLAIM)))
            .readonly(PCSCase::getFeatureFlags, NEVER_SHOW, true)
            .label("documentsYouveUploadedChecklist-separator", "---")
            .mandatory(PCSCase::getDocumentsYouveUploaded)
            .label("documentsYouveUploadedChecklist-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }
}
