package uk.gov.hmcts.reform.pcs.hearings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.hearings.model.DeleteHearingRequest;
import uk.gov.hmcts.reform.pcs.hearings.model.GetHearingsResponse;
import uk.gov.hmcts.reform.pcs.hearings.model.HearingRequest;
import uk.gov.hmcts.reform.pcs.hearings.model.HearingResponse;
import uk.gov.hmcts.reform.pcs.hearings.model.UpdateHearingRequest;
import uk.gov.hmcts.reform.pcs.hearings.service.api.HmcHearingApi;

class HmcHearingServiceTest {

    @Mock
    private HmcHearingApi hmcHearingApi;

    @Mock
    private AuthTokenGenerator authTokenGenerator;

    private HmcHearingService hmcHearingService;

    private static final String AUTH_HEADER = "Bearer token";
    private static final String SERVICE_AUTH_HEADER = "ServiceAuthToken";
    private static final String DEPLOYMENT_ID = null;
    private static final String HEARING_ID = "123";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH_HEADER);
        hmcHearingService = new HmcHearingService(hmcHearingApi, authTokenGenerator);
    }

    @Test
    void shouldCreateHearing() {
        HearingRequest request = new HearingRequest();
        HearingResponse response = new HearingResponse();

        when(hmcHearingApi.createHearing(AUTH_HEADER,
                                         SERVICE_AUTH_HEADER, DEPLOYMENT_ID, request)).thenReturn(response);

        HearingResponse result = hmcHearingService.createHearing(AUTH_HEADER, request);

        assertThat(result).isNotNull().isEqualTo(response);
        verify(hmcHearingApi, times(1)).createHearing(AUTH_HEADER,
                SERVICE_AUTH_HEADER, DEPLOYMENT_ID, request);
    }

    @Test
    void shouldUpdateHearing() {
        UpdateHearingRequest request = new UpdateHearingRequest();
        HearingResponse response = new HearingResponse();

        when(hmcHearingApi.updateHearing(AUTH_HEADER,
            SERVICE_AUTH_HEADER, DEPLOYMENT_ID, HEARING_ID, request)).thenReturn(response);

        HearingResponse result = hmcHearingService.updateHearing(AUTH_HEADER, HEARING_ID, request);

        assertThat(result).isNotNull().isEqualTo(response);
        verify(hmcHearingApi, times(1)).updateHearing(AUTH_HEADER,
            SERVICE_AUTH_HEADER, DEPLOYMENT_ID, HEARING_ID, request);
    }

    @Test
    void shouldDeleteHearing() {
        DeleteHearingRequest request = new DeleteHearingRequest();
        HearingResponse response = new HearingResponse();

        // Ensure that the API method is mocked correctly
        when(hmcHearingApi.deleteHearing(AUTH_HEADER,
                                         SERVICE_AUTH_HEADER, DEPLOYMENT_ID, HEARING_ID, request)).thenReturn(response);

        // Call the service method that triggers the API method
        HearingResponse result = hmcHearingService.deleteHearing(AUTH_HEADER,
                                                                 HEARING_ID, request);

        // Debugging output to check if the result is null
        System.out.println("Delete Hearing Response: " + result);
        System.out.println("Deployment ID used: " + DEPLOYMENT_ID);

        // Check that the result is not null and equals the expected response
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(response);

        // Verify that the deleteHearing method was called once with the correct arguments
        verify(hmcHearingApi, times(1)).deleteHearing(AUTH_HEADER,
                                                      SERVICE_AUTH_HEADER, DEPLOYMENT_ID, HEARING_ID, request);
    }

    @Test
    void shouldGetHearing() {
        GetHearingsResponse response = new GetHearingsResponse();

        when(hmcHearingApi.getHearing(AUTH_HEADER,
                                      SERVICE_AUTH_HEADER, DEPLOYMENT_ID, HEARING_ID, null)).thenReturn(response);

        GetHearingsResponse result = hmcHearingService.getHearing(AUTH_HEADER, HEARING_ID);

        assertThat(result).isNotNull().isEqualTo(response);
        verify(hmcHearingApi, times(1)).getHearing(AUTH_HEADER,
                                                   SERVICE_AUTH_HEADER, DEPLOYMENT_ID, HEARING_ID, null);
    }
}
