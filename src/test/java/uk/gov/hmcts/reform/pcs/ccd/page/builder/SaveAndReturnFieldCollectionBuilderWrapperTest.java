package uk.gov.hmcts.reform.pcs.ccd.page.builder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.ccd.sdk.api.FieldCollection.FieldCollectionBuilder;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SaveAndReturnFieldCollectionBuilderWrapperTest {

    private static final String PAGE_ID = "testPage";
    private static final String EXPECTED_LABEL_ID = PAGE_ID + "-saveAndReturn";

    @Mock
    private FieldCollectionBuilder<PCSCase, State, EventBuilder<PCSCase, UserRole, State>> pageBuilder;

    @Test
    void shouldAddSaveAndReturnLabelWithCorrectId() {
        // When
        SaveAndReturnFieldCollectionBuilderWrapper.addSaveAndReturnLabel(pageBuilder, PAGE_ID);

        // Then
        verify(pageBuilder).label(eq(EXPECTED_LABEL_ID), eq(CommonPageContent.SAVE_AND_RETURN));
    }

    @Test
    void shouldAddSaveAndReturnLabelWithCorrectContent() {
        // When
        SaveAndReturnFieldCollectionBuilderWrapper.addSaveAndReturnLabel(pageBuilder, PAGE_ID);

        // Then
        verify(pageBuilder).label(eq(EXPECTED_LABEL_ID), eq(CommonPageContent.SAVE_AND_RETURN));
    }

    @Test
    void shouldGenerateCorrectLabelIdForDifferentPageIds() {
        // Given
        String pageId1 = "noticeDetails";
        String pageId2 = "claimantDetails";

        // When
        SaveAndReturnFieldCollectionBuilderWrapper.addSaveAndReturnLabel(pageBuilder, pageId1);
        SaveAndReturnFieldCollectionBuilderWrapper.addSaveAndReturnLabel(pageBuilder, pageId2);

        // Then
        verify(pageBuilder).label(eq("noticeDetails-saveAndReturn"), eq(CommonPageContent.SAVE_AND_RETURN));
        verify(pageBuilder).label(eq("claimantDetails-saveAndReturn"), eq(CommonPageContent.SAVE_AND_RETURN));
    }
}

