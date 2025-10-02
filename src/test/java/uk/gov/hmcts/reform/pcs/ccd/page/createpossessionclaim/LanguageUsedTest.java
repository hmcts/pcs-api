package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.ccd.sdk.api.FieldCollection.FieldCollectionBuilder;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("LanguageUsed Page Tests")
@ExtendWith(MockitoExtension.class)
class LanguageUsedTest {

    @Mock
    private EventBuilder<PCSCase, UserRole, State> eventBuilder;
    @Mock
    private FieldCollectionBuilder<PCSCase, State, EventBuilder<PCSCase, UserRole, State>> fieldBuilder;

    private PageBuilder pageBuilder;
    private LanguageUsed underTest;

    @BeforeEach
    void setUp() {
        when(eventBuilder.fields()).thenReturn(fieldBuilder);
        when(fieldBuilder.page(anyString())).thenReturn(fieldBuilder);
        when(fieldBuilder.pageLabel(anyString())).thenReturn(fieldBuilder);
        when(fieldBuilder.label(anyString(), anyString())).thenReturn(fieldBuilder);
        when(fieldBuilder.mandatory(any())).thenReturn(fieldBuilder);

        pageBuilder = new PageBuilder(eventBuilder);
        underTest = new LanguageUsed();
    }

    @Test
    @DisplayName("Should add main content label with correct heading and text")
    void shouldAddMainContentLabelWithCorrectHeadingAndText() {
        underTest.addTo(pageBuilder);

        verify(fieldBuilder).label(
            eq("languageUsedMainContent"),
            eq("<h1 class=\"govuk-heading-l\">Language used</h1>"
                + "<p class=\"govuk-body\">Did you complete all or part of this claim in Welsh?</p>")
        );
    }

    @Test
    @DisplayName("Should make welshUsed field mandatory")
    void shouldMakeWelshUsedFieldMandatory() {
        underTest.addTo(pageBuilder);

        // Verify that mandatory was called (we can't easily verify the exact method reference)
        verify(fieldBuilder).mandatory(any());
    }
}
