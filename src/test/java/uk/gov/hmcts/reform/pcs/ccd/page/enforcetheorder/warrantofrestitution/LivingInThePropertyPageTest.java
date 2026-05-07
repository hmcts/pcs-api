package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrantofrestitution;

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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType.WARRANT_OF_RESTITUTION_FLOW;

@DisplayName("LivingInThePropertyPage (warrant of restitution) tests")
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class LivingInThePropertyPageTest {

    @Mock
    private EventBuilder<PCSCase, UserRole, State> eventBuilder;

    @Mock
    @SuppressWarnings("rawtypes")
    private FieldCollectionBuilder fieldBuilder;

    private PageBuilder pageBuilder;

    private LivingInThePropertyPage underTest;

    @BeforeEach
    void setUp() {
        lenient().when(eventBuilder.fields()).thenReturn(fieldBuilder);
        lenient().when(fieldBuilder.page(anyString())).thenReturn(fieldBuilder);
        lenient().when(fieldBuilder.pageLabel(anyString())).thenReturn(fieldBuilder);
        lenient().when(fieldBuilder.showCondition(anyString())).thenReturn(fieldBuilder);
        lenient().when(fieldBuilder.label(anyString(), anyString())).thenReturn(fieldBuilder);
        lenient().when(fieldBuilder.complex(any())).thenReturn(fieldBuilder);
        lenient().when(fieldBuilder.mandatory(any())).thenReturn(fieldBuilder);
        lenient().when(fieldBuilder.done()).thenReturn(fieldBuilder);

        pageBuilder = new PageBuilder(eventBuilder);
        underTest = new LivingInThePropertyPage();
    }

    @Test
    @DisplayName("Should configure page id, label and restitution flow show condition")
    void shouldConfigurePageMetadataAndShowCondition() {
        underTest.addTo(pageBuilder);

        verify(fieldBuilder).page("livingInThePropertyRestitution");
        verify(fieldBuilder).pageLabel("Everyone living at the property");
        verify(fieldBuilder).showCondition(WARRANT_OF_RESTITUTION_FLOW);
        verify(fieldBuilder).label("livingInThePropertyRestitution-content", "---");
    }
}

