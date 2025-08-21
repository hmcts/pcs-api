package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Rent Details Page Tests")
class RentDetailsTest {

    private EventBuilder<PCSCase, UserRole, State> eventBuilder;
    private FieldCollectionBuilder<PCSCase, State, EventBuilder<PCSCase, UserRole, State>> fieldBuilder;
    private PageBuilder pageBuilder;
    private RentDetails underTest;

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
        when(fieldBuilder.mandatory(any(), anyString())).thenReturn(fieldBuilder);

        pageBuilder = new PageBuilder(eventBuilder);
        underTest = new RentDetails();
    }

    @Test
    @DisplayName("Should configure page basics")
    void shouldConfigurePageBasics() {
        underTest.addTo(pageBuilder);

        verify(fieldBuilder).page(eq("rentDetails"));
        verify(fieldBuilder).pageLabel(eq("Rent details"));
    }

    @Test
    @DisplayName("Should configure rent details content")
    void shouldConfigureRentDetailsContent() {
        underTest.addTo(pageBuilder);

        verify(fieldBuilder).label(eq("rentDetails-content"), argThat(content
                -> content.contains("Please provide details about the current rental agreement")
        ));
    }

    @Test
    @DisplayName("Should configure all mandatory fields")
    void shouldConfigureAllMandatoryFields() {
        underTest.addTo(pageBuilder);

        // Verify exactly 4 mandatory fields are configured
        verify(fieldBuilder, times(2)).mandatory(any()); // rentAmount and rentPaymentFrequency
        verify(fieldBuilder, times(2)).mandatory(any(), eq("rentPaymentFrequency=\"OTHER\"")); 
        // conditional fields
    }

    @Test
    @DisplayName("Should configure conditional fields for 'Other' selection")
    void shouldConfigureConditionalFieldsForOtherSelection() {
        underTest.addTo(pageBuilder);

        // Verify conditional fields are configured with correct show conditions
        verify(fieldBuilder, times(2)).mandatory(any(), eq("rentPaymentFrequency=\"OTHER\"")); 
        // otherRentFrequency and dailyRentChargeAmount
    }
} 