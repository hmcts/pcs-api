package uk.gov.hmcts.reform.pcs.hearings.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.pcs.hearings.model.DeleteHearingRequest;
import uk.gov.hmcts.reform.pcs.hearings.model.GetHearingsResponse;
import uk.gov.hmcts.reform.pcs.hearings.model.HearingRequest;
import uk.gov.hmcts.reform.pcs.hearings.model.HearingResponse;
import uk.gov.hmcts.reform.pcs.hearings.model.UpdateHearingRequest;
import uk.gov.hmcts.reform.pcs.hearings.service.HmcHearingService;

class HearingsControllerTest {

    @Mock
    private HmcHearingService hmcHearingService;

    @InjectMocks
    private HearingsController hearingsController;

    private static final String AUTH_HEADER = "Bearer token";
    private static final String SERVICE_AUTH_HEADER = "ServiceAuthToken";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldCreateHearing() {
        HearingRequest request = new HearingRequest();
        HearingResponse response = new HearingResponse();

        when(hmcHearingService.createHearing(AUTH_HEADER, request)).thenReturn(response);

        HearingResponse result = hearingsController.createHearing(AUTH_HEADER, SERVICE_AUTH_HEADER, request);

        assertThat(result).isEqualTo(response);
        verify(hmcHearingService, times(1)).createHearing(AUTH_HEADER, request);
    }

    @Test
    void shouldUpdateHearing() {
        UpdateHearingRequest request = new UpdateHearingRequest();
        HearingResponse response = new HearingResponse();

        when(hmcHearingService.updateHearing(AUTH_HEADER, "123", request)).thenReturn(response);

        HearingResponse result = hearingsController.updateHearing(AUTH_HEADER, SERVICE_AUTH_HEADER, "123", request);

        assertThat(result).isEqualTo(response);
        verify(hmcHearingService, times(1)).updateHearing(AUTH_HEADER, "123", request);
    }

    @Test
    void shouldDeleteHearing() {
        DeleteHearingRequest request = new DeleteHearingRequest();
        HearingResponse response = new HearingResponse();

        when(hmcHearingService.deleteHearing(AUTH_HEADER, "123", request)).thenReturn(response);

        HearingResponse result = hearingsController.deleteHearing(AUTH_HEADER, SERVICE_AUTH_HEADER, "123", request);

        assertThat(result).isEqualTo(response);
        verify(hmcHearingService, times(1)).deleteHearing(AUTH_HEADER, "123", request);
    }

    @Test
    void shouldGetHearing() {
        GetHearingsResponse response = new GetHearingsResponse();

        when(hmcHearingService.getHearing(AUTH_HEADER, "123")).thenReturn(response);

        GetHearingsResponse result = hearingsController.getHearing(AUTH_HEADER, SERVICE_AUTH_HEADER, "123");

        assertThat(result).isEqualTo(response);
        verify(hmcHearingService, times(1)).getHearing(AUTH_HEADER, "123");
    }
}
