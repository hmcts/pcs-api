package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventTypeBuilder;
import uk.gov.hmcts.ccd.sdk.api.FieldCollection;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.AWAITING_SUBMISSION_TO_HMCTS;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.createTestCase;
import static uk.gov.hmcts.reform.pcs.ccd.event.NonProdSupport.EVENT_NAME;

@ExtendWith(MockitoExtension.class)
class NonProdSupportTest {

    @InjectMocks
    private NonProdSupport underTest;

    @Test
    @SuppressWarnings("unchecked")
    void shouldSuccessfullyConfigureDecentralisedEvent() {
        // Given
        DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder = mock(DecentralisedConfigBuilder.class);
        EventTypeBuilder<PCSCase, UserRole, State> typeBuilder = mock(EventTypeBuilder.class);
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder = mock(Event.EventBuilder.class,
                                                                         Answers.RETURNS_SELF);
        FieldCollection.FieldCollectionBuilder<PCSCase, State, Event.EventBuilder<PCSCase, UserRole, State>> f =
            mock(FieldCollection.FieldCollectionBuilder.class, Answers.RETURNS_SELF);
        doReturn(typeBuilder).when(configBuilder).decentralisedEvent(anyString(), any(), any());
        when(typeBuilder.initialState(any())).thenReturn(eventBuilder);
        when(eventBuilder.fields()).thenReturn(f);

        // When
        underTest.configure(configBuilder);

        // Then
        verify(configBuilder).decentralisedEvent(eq(createTestCase.name()), any(), any());
        verify(typeBuilder).initialState(AWAITING_SUBMISSION_TO_HMCTS);
        verify(eventBuilder).showSummary();
        verify(eventBuilder).name(EVENT_NAME);
        verify(eventBuilder).grant(Permission.CRUD, UserRole.PCS_SOLICITOR);
    }

}
