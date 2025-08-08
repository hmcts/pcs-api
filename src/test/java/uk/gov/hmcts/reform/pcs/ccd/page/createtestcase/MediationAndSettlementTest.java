package uk.gov.hmcts.reform.pcs.ccd.page.createtestcase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.ccd.sdk.api.FieldCollection.FieldCollectionBuilder;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the Mediation and Settlement page configuration.
 * Verifies page structure, field configuration, validation, and accessibility requirements.
 */
class MediationAndSettlementTest {

    private EventBuilder<PCSCase, UserRole, State> eventBuilder;
    private FieldCollectionBuilder<PCSCase, State, EventBuilder<PCSCase, UserRole, State>> fieldBuilder;
    private PageBuilder pageBuilder;
    private MediationAndSettlement underTest;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        eventBuilder = mock(EventBuilder.class);
        fieldBuilder = mock(FieldCollectionBuilder.class);

        when(eventBuilder.fields()).thenReturn(fieldBuilder);
        when(fieldBuilder.page(anyString())).thenReturn(fieldBuilder);
        when(fieldBuilder.pageLabel(anyString())).thenReturn(fieldBuilder);
        when(fieldBuilder.label(anyString(), anyString())).thenReturn(fieldBuilder);
        when(fieldBuilder.mandatory(any())).thenReturn(fieldBuilder);
        when(fieldBuilder.optional(any(), anyString())).thenReturn(fieldBuilder);

        pageBuilder = new PageBuilder(eventBuilder);
        underTest = new MediationAndSettlement();
    }

    @Test
    void shouldConfigurePageBasics() {
        underTest.addTo(pageBuilder);

        verify(fieldBuilder).page(eq("mediationAndSettlement"));
        verify(fieldBuilder).pageLabel(eq("Mediation and settlement"));
    }

    @Test
    void shouldConfigureMediationSection() {
        underTest.addTo(pageBuilder);

        InOrder inOrder = inOrder(fieldBuilder);

        // Verify mediation description and question
        String mediationIntro = "Mediation is when an impartial professional";
        inOrder.verify(fieldBuilder).label(eq("mediationAndSettlement-content"), argThat(content
                -> content.contains(mediationIntro)
        ));

        // Verify mediation fields
        inOrder.verify(fieldBuilder).mandatory(any());
        inOrder.verify(fieldBuilder).optional(any(), eq("mediation=\"YES\""));
    }

    @Test
    void shouldConfigureSettlementSection() {
        underTest.addTo(pageBuilder);

        InOrder inOrder = inOrder(fieldBuilder);

        // First verify mediation section (comes before settlement)
        inOrder.verify(fieldBuilder).label(eq("mediationAndSettlement-content"), anyString());
        inOrder.verify(fieldBuilder).mandatory(any());
        inOrder.verify(fieldBuilder).optional(any(), anyString());

        // Then verify settlement section
        String content = "If your claim is on the grounds of rent arrears";
        inOrder.verify(fieldBuilder).label(eq("settlement-section"), argThat(text
                -> text.contains(content)
        ));
        inOrder.verify(fieldBuilder).mandatory(any());
        inOrder.verify(fieldBuilder).optional(any(), eq("settlement=\"YES\""));
    }

    @Test
    void shouldRequireMediationAndSettlementSelections() {
        underTest.addTo(pageBuilder);

        // Verify exactly two mandatory fields are configured
        verify(fieldBuilder, times(2)).mandatory(any());
    }

    @Test
    void shouldMakeDetailFieldsOptional() {
        underTest.addTo(pageBuilder);

        // Verify exactly two optional fields are configured
        verify(fieldBuilder, times(2)).optional(any(), anyString());
    }

    @Test
    void shouldShowDetailFieldsOnlyWhenYesSelected() {
        underTest.addTo(pageBuilder);

        // Verify the show conditions for both optional fields
        verify(fieldBuilder).optional(any(), eq("mediation=\"YES\""));
        verify(fieldBuilder).optional(any(), eq("settlement=\"YES\""));
    }

    @Test
    void shouldIncludeSectionSeparators() {
        underTest.addTo(pageBuilder);

        // Verify both sections start with a separator
        verify(fieldBuilder, times(2)).label(anyString(), argThat(content -> 
            content.trim().startsWith("---")
        ));
    }
}