package uk.gov.hmcts.reform.pcs.hearings.endpoint;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.hearings.model.DeleteHearingRequest;
import uk.gov.hmcts.reform.pcs.hearings.model.GetHearingsResponse;
import uk.gov.hmcts.reform.pcs.hearings.model.HearingRequest;
import uk.gov.hmcts.reform.pcs.hearings.model.HearingResponse;
import uk.gov.hmcts.reform.pcs.hearings.model.UpdateHearingRequest;
import uk.gov.hmcts.reform.pcs.hearings.service.HmcHearingService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HearingsControllerTest {

    @Mock
    private HmcHearingService hmcHearingService;

    @InjectMocks
    private HearingsController hearingsController;

    private static final String AUTH_HEADER = "Bearer token";
    private static final String SERVICE_AUTH_HEADER = "ServiceAuthToken";

    @Test
    void shouldCreateHearing() {
        HearingRequest request = new HearingRequest();
        HearingResponse response = new HearingResponse();

        when(hmcHearingService.createHearing(request)).thenReturn(response);

        HearingResponse result = hearingsController.createHearing(AUTH_HEADER, SERVICE_AUTH_HEADER, request);

        assertThat(result).isEqualTo(response);
        verify(hmcHearingService).createHearing(request);
    }

    @Test
    void shouldUpdateHearing() {
        UpdateHearingRequest request = new UpdateHearingRequest();
        HearingResponse response = new HearingResponse();

        when(hmcHearingService.updateHearing("123", request)).thenReturn(response);

        HearingResponse result = hearingsController.updateHearing(AUTH_HEADER, SERVICE_AUTH_HEADER, "123", request);

        assertThat(result).isEqualTo(response);
        verify(hmcHearingService).updateHearing("123", request);
    }

    @Test
    void shouldDeleteHearing() {
        DeleteHearingRequest request = new DeleteHearingRequest();
        HearingResponse response = new HearingResponse();

        when(hmcHearingService.deleteHearing("123", request)).thenReturn(response);

        HearingResponse result = hearingsController.deleteHearing(AUTH_HEADER, SERVICE_AUTH_HEADER, "123", request);

        assertThat(result).isEqualTo(response);
        verify(hmcHearingService).deleteHearing("123", request);
    }

    @Test
    void shouldGetHearing() {
        GetHearingsResponse response = new GetHearingsResponse();

        when(hmcHearingService.getHearing("123")).thenReturn(response);

        GetHearingsResponse result = hearingsController.getHearing(AUTH_HEADER, SERVICE_AUTH_HEADER, "123");

        assertThat(result).isEqualTo(response);
        verify(hmcHearingService).getHearing("123");
    }
}
