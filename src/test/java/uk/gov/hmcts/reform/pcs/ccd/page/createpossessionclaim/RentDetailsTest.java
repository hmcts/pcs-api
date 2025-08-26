package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Rent Details Page Tests")
@ExtendWith(MockitoExtension.class)
class RentDetailsTest {

    @Mock
    private EventBuilder<PCSCase, UserRole, State> eventBuilder;
    
    @Mock
    private FieldCollectionBuilder<PCSCase, State, EventBuilder<PCSCase, UserRole, State>> fieldBuilder;
    
    private PageBuilder pageBuilder;
    private RentDetails underTest;

    @BeforeEach
    void setUp() {
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
    @DisplayName("Should configure conditional fields for 'Other' selection")
    void shouldConfigureConditionalFieldsForOtherSelection() {
        underTest.addTo(pageBuilder);

        // Verify conditional fields are configured with correct show conditions
        verify(fieldBuilder).mandatory(any(), eq("rentFrequency!=\"OTHER\"")); // currentRent
        verify(fieldBuilder, times(2)).mandatory(any(), eq("rentFrequency=\"OTHER\"")); 
        // otherRentFrequency and dailyRentChargeAmount
    }
} 