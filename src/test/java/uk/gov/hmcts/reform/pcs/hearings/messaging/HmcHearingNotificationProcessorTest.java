package uk.gov.hmcts.reform.pcs.hearings.messaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.hearings.model.HmcHearingResponse;
import uk.gov.hmcts.reform.pcs.hearings.service.HmcHearingService;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HmcHearingNotificationProcessorTest {

    @Mock
    private HmcHearingService hmcHearingService;

    @InjectMocks
    private HmcHearingNotificationProcessor processor;

    @Test
    void shouldRetrieveHearingFromHmc() {
        // given
        HmcHearingResponse notification = new HmcHearingResponse();
        notification.setHearingID("2000000001");

        // when
        processor.process(notification);

        // then
        verify(hmcHearingService).getHearing("2000000001");
    }

    @Test
    void shouldPropagateHmcFailure() {
        // given
        HmcHearingResponse notification = new HmcHearingResponse();
        notification.setHearingID("2000000001");
        when(hmcHearingService.getHearing("2000000001")).thenThrow(new IllegalStateException("HMC unavailable"));

        // when
        // then
        assertThatThrownBy(() -> processor.process(notification))
            .isInstanceOf(IllegalStateException.class);
    }
}
