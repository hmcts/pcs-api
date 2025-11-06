package uk.gov.hmcts.reform.pcs.ccd.page.builder;

import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.ccd.sdk.api.FieldCollection.FieldCollectionBuilder;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

/**
 * Utility class for adding the save and return label to pages.
 */
public final class SaveAndReturnFieldCollectionBuilderWrapper {

    private static final String SAVE_AND_RETURN_LABEL_ID_SUFFIX = "-saveAndReturn";

    private SaveAndReturnFieldCollectionBuilderWrapper() {
        // Utility class - prevent instantiation
    }

    /**
     * Adds the save and return label to the given page builder.
     * This should be called after all page content has been added to ensure
     * the label appears at the bottom of the page, before the navigation buttons.
     */
    public static void addSaveAndReturnLabel(
        FieldCollectionBuilder<PCSCase, State, EventBuilder<PCSCase, UserRole, State>> pageBuilder,
        String pageId) {
        String labelId = pageId + SAVE_AND_RETURN_LABEL_ID_SUFFIX;
        pageBuilder.label(labelId, CommonPageContent.SAVE_AND_RETURN);
    }
}
