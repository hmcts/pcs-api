package uk.gov.hmcts.reform.pcs.ccd.event.genapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.event.EventStates;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.RELEASE_1_DOT_3;

@ExtendWith(MockitoExtension.class)
class MakeAnApplicationTest extends BaseEventTest {

    @Mock
    private StartEventHandler startEventHandler;
    @Mock
    private SubmitEventHandler submitEventHandler;
    @Mock
    private FeatureToggleService featureToggleService;
    @Captor
    private ArgumentCaptor<EventPayload<PCSCase, State>> eventPayloadCaptor;

    @BeforeEach
    void setUp() {
        when(featureToggleService.isEnabled(RELEASE_1_DOT_3)).thenReturn(true);
        MakeAnApplication underTest = new MakeAnApplication(startEventHandler, submitEventHandler, featureToggleService);

        setEventUnderTest(underTest);
    }

    @Test
    void shouldBeConfiguredForEventStates() {
        assertConfiguredForStates(EventStates.makeAnApplication());
    }

    @Test
    void shouldBeConfiguredForAllStatesWhenRelease1dot3FeatureFlagDisabled() {
        when(featureToggleService.isEnabled(RELEASE_1_DOT_3)).thenReturn(false);
        setEventUnderTest(new MakeAnApplication(startEventHandler, submitEventHandler, featureToggleService));

        assertConfiguredForAllStates();
    }

    @Test
    void shouldCallStartEventHandler() {
        // Given
        PCSCase caseData = mock(PCSCase.class);

        // When
        callStartHandler(caseData);

        // Then
        verify(startEventHandler).start(eventPayloadCaptor.capture());
        assertThat(eventPayloadCaptor.getValue().caseData()).isEqualTo(caseData);
    }

    @Test
    void shouldCallSubmitEventHandler() {
        // Given
        PCSCase caseData = mock(PCSCase.class);

        // When
        callSubmitHandler(caseData);

        // Then
        verify(submitEventHandler).submit(eventPayloadCaptor.capture());
        assertThat(eventPayloadCaptor.getValue().caseData()).isEqualTo(caseData);
    }

}
