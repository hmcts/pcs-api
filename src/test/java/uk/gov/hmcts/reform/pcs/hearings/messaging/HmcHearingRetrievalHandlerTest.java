package uk.gov.hmcts.reform.pcs.hearings.messaging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.pcs.hearings.model.HmcHearingResponse;
import uk.gov.hmcts.reform.pcs.hearings.service.HmcHearingService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HmcHearingRetrievalHandlerTest {

    @Mock
    private HmcHearingService hmcHearingService;

    private HmcHearingRetrievalHandler handler;
    private Logger handlerLogger;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        handler = new HmcHearingRetrievalHandler(hmcHearingService);
        handlerLogger = (Logger) LoggerFactory.getLogger(HmcHearingRetrievalHandler.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        handlerLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        handlerLogger.detachAppender(logAppender);
    }

    @Test
    void shouldRetrieveHearingFromHmcAndNothingElse() {
        // given
        HmcHearingResponse notification = notification();

        // when
        handler.handle(notification);

        // then
        verify(hmcHearingService).getHearing("2000000001");
        verifyNoMoreInteractions(hmcHearingService);
    }

    @Test
    void shouldLogRetrievedHearingAndCase() {
        // given
        HmcHearingResponse notification = notification();

        // when
        handler.handle(notification);

        // then
        assertThat(logAppender.list.getFirst().getFormattedMessage())
            .contains("2000000001", "1234567890123456");
    }

    @Test
    void shouldPropagateHmcFailure() {
        // given
        HmcHearingResponse notification = notification();
        when(hmcHearingService.getHearing("2000000001")).thenThrow(new IllegalStateException("HMC unavailable"));

        // when
        // then
        assertThatThrownBy(() -> handler.handle(notification))
            .isInstanceOf(IllegalStateException.class);
        assertThat(logAppender.list).isEmpty();
    }

    private static HmcHearingResponse notification() {
        HmcHearingResponse notification = new HmcHearingResponse();
        notification.setHearingID("2000000001");
        notification.setCaseRef("1234567890123456");
        return notification;
    }
}
