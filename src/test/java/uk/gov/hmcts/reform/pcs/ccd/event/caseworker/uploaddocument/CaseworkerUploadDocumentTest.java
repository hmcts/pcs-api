package uk.gov.hmcts.reform.pcs.ccd.event.caseworker.uploaddocument;

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

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CaseworkerUploadDocumentTest extends BaseEventTest {

    @Mock
    private StartHandler startHandler;
    @Mock
    private SubmitHandler submitHandler;

    @Captor
    private ArgumentCaptor<EventPayload<PCSCase, State>> eventPayloadCaptor;

    @BeforeEach
    void setUp() {
        CaseworkerUploadDocument underTest = new CaseworkerUploadDocument(
            startHandler,
            submitHandler,
            Clock.systemUTC()
        );

        setEventUnderTest(underTest);
    }

    @Test
    void shouldCallStartEventHandler() {
        // Given
        PCSCase caseData = mock(PCSCase.class);

        // When
        callStartHandler(caseData);

        // Then
        verify(startHandler).start(eventPayloadCaptor.capture());
        assertThat(eventPayloadCaptor.getValue().caseData()).isEqualTo(caseData);
    }

    @Test
    void shouldCallSubmitEventHandler() {
        // Given
        PCSCase caseData = mock(PCSCase.class);

        // When
        callSubmitHandler(caseData);

        // Then
        verify(submitHandler).submit(eventPayloadCaptor.capture());
        assertThat(eventPayloadCaptor.getValue().caseData()).isEqualTo(caseData);
    }
}
