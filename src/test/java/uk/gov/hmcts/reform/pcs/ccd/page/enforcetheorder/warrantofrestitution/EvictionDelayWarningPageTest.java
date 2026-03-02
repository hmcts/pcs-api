package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrantofrestitution;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.ccd.sdk.api.FieldCollection.FieldCollectionBuilder;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType.WARRANT_OF_RESTITUTION_FLOW;

@DisplayName("EvictionDelayWarningPage tests")
@ExtendWith(MockitoExtension.class)
class EvictionDelayWarningPageTest {

    @Mock
    private EventBuilder<PCSCase, UserRole, State> eventBuilder;

    @Mock
    private FieldCollectionBuilder<PCSCase, State, EventBuilder<PCSCase, UserRole, State>> fieldBuilder;

    private PageBuilder pageBuilder;

    private EvictionDelayWarningPage underTest;

    @BeforeEach
    void setUp() {
        lenient().when(eventBuilder.fields()).thenReturn(fieldBuilder);
        lenient().when(fieldBuilder.page(anyString())).thenReturn(fieldBuilder);
        lenient().when(fieldBuilder.pageLabel(anyString())).thenReturn(fieldBuilder);
        lenient().when(fieldBuilder.showCondition(anyString())).thenReturn(fieldBuilder);
        lenient().when(fieldBuilder.label(anyString(), anyString())).thenReturn(fieldBuilder);

        pageBuilder = new PageBuilder(eventBuilder);
        underTest = new EvictionDelayWarningPage();
    }

    @Test
    @DisplayName("Should configure page id, label and restitution risk routing condition")
    void shouldConfigurePageMetadataAndRoutingCondition() {
        underTest.addTo(pageBuilder);

        verify(fieldBuilder).page("warrantOfRestitutionEvictionDelayWarning");
        verify(fieldBuilder).pageLabel(
            "The eviction could be delayed if the bailiff identifies a risk on the day"
        );

        String expectedShowCondition =
            WARRANT_OF_RESTITUTION_FLOW
                + " AND (warrantOfRestitutionAnyRiskToBailiff=\"NO\" "
                + "OR warrantOfRestitutionAnyRiskToBailiff=\"NOT_SURE\")";

        verify(fieldBuilder).showCondition(expectedShowCondition);
        verify(fieldBuilder).label("warrantOfRestitutionEvictionDelayWarning-line-separator", "---");
    }

    @Test
    @DisplayName("Should configure warning body label with expected content")
    void shouldConfigureWarningBodyLabelContent() {
        underTest.addTo(pageBuilder);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(fieldBuilder, atLeastOnce()).label(anyString(), bodyCaptor.capture());

        String body = bodyCaptor.getValue();
        assertNotNull(body);
        assertThat(body).contains(
            "govuk-warning-text",
            "may not be able to carry out the eviction",
            "dangerous dog on the premises"
        );
    }
}

