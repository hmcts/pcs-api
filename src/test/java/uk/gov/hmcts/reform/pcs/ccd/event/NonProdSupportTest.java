package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventTypeBuilder;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.nonprod.CaseSupportHelper;
import uk.gov.hmcts.reform.pcs.ccd.service.nonprod.NonProdSupportService;

import static org.mockito.Answers.RETURNS_SELF;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.sdk.api.Permission.CRUD;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.AWAITING_SUBMISSION_TO_HMCTS;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.createTestCase;
import static uk.gov.hmcts.reform.pcs.ccd.event.NonProdSupport.EVENT_NAME;

@ExtendWith(MockitoExtension.class)
class NonProdSupportTest {

    @InjectMocks
    private NonProdSupport underTest;

    @Mock
    private NonProdSupportService nonProdSupportService;
    @Mock
    private CaseSupportHelper caseSupportHelper;

    @Test
    @SuppressWarnings("unchecked")
    void shouldSuccessfullyConfigureDecentralisedEventWithCorrectEventName() {
        // Given
        DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder = mock(DecentralisedConfigBuilder.class);

        EventTypeBuilder<PCSCase, UserRole, State> typeBuilder = mock(EventTypeBuilder.class);
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder = mock(Event.EventBuilder.class, Answers.RETURNS_SELF);

        when(configBuilder.decentralisedEvent(anyString(), any(), any())).thenReturn(typeBuilder);
        when(typeBuilder.forState(any())).thenReturn(eventBuilder);
        when(typeBuilder.forAllStates()).thenReturn(eventBuilder);

        doReturn(eventBuilder).when(configBuilder).decentralisedEvent(anyString(), any(), any());

        // When
        underTest.configure(configBuilder);

        // Then
        verify(configBuilder).decentralisedEvent(eq(createTestCase.name()), any(), any());
        verify(eventBuilder).showSummary();
        verify(eventBuilder).name(EVENT_NAME);
        verify(eventBuilder).grant(CRUD, UserRole.PCS_SOLICITOR);
    }

}
