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

@DisplayName("LivingInThePropertyIntroPage tests")
@ExtendWith(MockitoExtension.class)
class LivingInThePropertyIntroPageTest {

    @Mock
    private EventBuilder<PCSCase, UserRole, State> eventBuilder;

    @Mock
    private FieldCollectionBuilder<PCSCase, State, EventBuilder<PCSCase, UserRole, State>> fieldBuilder;

    private PageBuilder pageBuilder;

    private LivingInThePropertyIntroPage underTest;

    @BeforeEach
    void setUp() {
        lenient().when(eventBuilder.fields()).thenReturn(fieldBuilder);
        lenient().when(fieldBuilder.page(anyString())).thenReturn(fieldBuilder);
        lenient().when(fieldBuilder.pageLabel(anyString())).thenReturn(fieldBuilder);
        lenient().when(fieldBuilder.showCondition(anyString())).thenReturn(fieldBuilder);
        lenient().when(fieldBuilder.label(anyString(), anyString())).thenReturn(fieldBuilder);

        pageBuilder = new PageBuilder(eventBuilder);
        underTest = new LivingInThePropertyIntroPage();
    }

    @Test
    @DisplayName("Should configure page id, label and restitution flow show condition")
    void shouldConfigurePageMetadataAndShowCondition() {
        underTest.addTo(pageBuilder);

        verify(fieldBuilder).page("warrantOfRestitutionAnyoneAtPropertyRiskIntro");
        verify(fieldBuilder).pageLabel(
            "On the next few questions, we will ask you to tell the bailiff if anyone at the property still "
                + "poses a risk"
        );
        verify(fieldBuilder).showCondition(WARRANT_OF_RESTITUTION_FLOW);
        verify(fieldBuilder).label("warrantOfRestitutionAnyoneAtPropertyRiskIntro-line-separator", "---");
    }

    @Test
    @DisplayName("Should configure intro body label with expected content")
    void shouldConfigureIntroBodyLabelContent() {
        underTest.addTo(pageBuilder);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(fieldBuilder, atLeastOnce()).label(anyString(), bodyCaptor.capture());

        String body = bodyCaptor.getValue();
        assertNotNull(body);
        assertThat(body).contains(
            "For example, we will ask you to:",
            "check your previous answers to the bailiff’s questions",
            "update your answers if something has changed",
            "prepare for the eviction"
        );
    }
}

