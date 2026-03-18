package uk.gov.hmcts.reform.pcs.hearings.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.hearings.model.DeleteHearingRequest;
import uk.gov.hmcts.reform.pcs.hearings.model.GetHearingsResponse;
import uk.gov.hmcts.reform.pcs.hearings.model.HearingRequest;
import uk.gov.hmcts.reform.pcs.hearings.model.HearingResponse;
import uk.gov.hmcts.reform.pcs.hearings.model.UpdateHearingRequest;
import uk.gov.hmcts.reform.pcs.hearings.service.api.HmcHearingApi;
import uk.gov.hmcts.reform.pcs.idam.IdamService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
class HmcHearingServiceTest {

    @Mock
    private HmcHearingApi hmcHearingApi;
    @Mock
    private AuthTokenGenerator authTokenGenerator;
    @Mock
    private IdamService idamService;

    private HmcHearingService hmcHearingService;

    private static final String AUTH_HEADER = "Bearer token";
    private static final String SERVICE_AUTH_HEADER = "ServiceAuthToken";
    private static final String DEPLOYMENT_ID = null;
    private static final String DATA_STORE_URL = "http://localhost:4452";
    private static final String ROLE_ASSIGNMENT_URL = "http://localhost:4096";
    private static final String HEARING_ID = "123";

    @BeforeEach
    void setUp() {
        when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH_HEADER);
        when(idamService.getSystemUserAuthorisation()).thenReturn(AUTH_HEADER);
        hmcHearingService = new HmcHearingService(hmcHearingApi, authTokenGenerator, idamService);
        setField(hmcHearingService, "dataStoreUrl", DATA_STORE_URL);
        setField(hmcHearingService, "roleAssignmentUrl", ROLE_ASSIGNMENT_URL);
    }

    @Test
    void shouldCreateHearing() {
        HearingRequest request = new HearingRequest();
        HearingResponse response = new HearingResponse();

        when(hmcHearingApi.createHearing(AUTH_HEADER, SERVICE_AUTH_HEADER, DEPLOYMENT_ID,
                                         DATA_STORE_URL, ROLE_ASSIGNMENT_URL, request)).thenReturn(response);

        HearingResponse result = hmcHearingService.createHearing(request);

        assertThat(result).isNotNull().isEqualTo(response);
        verify(hmcHearingApi).createHearing(AUTH_HEADER, SERVICE_AUTH_HEADER, DEPLOYMENT_ID,
                                            DATA_STORE_URL, ROLE_ASSIGNMENT_URL, request);
    }

    @Test
    void shouldUpdateHearing() {
        UpdateHearingRequest request = new UpdateHearingRequest();
        HearingResponse response = new HearingResponse();

        when(hmcHearingApi.updateHearing(AUTH_HEADER, SERVICE_AUTH_HEADER, DEPLOYMENT_ID, DATA_STORE_URL,
            ROLE_ASSIGNMENT_URL, HEARING_ID, request)).thenReturn(response);

        HearingResponse result = hmcHearingService.updateHearing(HEARING_ID, request);

        assertThat(result).isNotNull().isEqualTo(response);
        verify(hmcHearingApi).updateHearing(AUTH_HEADER, SERVICE_AUTH_HEADER, DEPLOYMENT_ID,
                                            DATA_STORE_URL, ROLE_ASSIGNMENT_URL, HEARING_ID, request);
    }

    @Test
    void shouldDeleteHearing() {
        DeleteHearingRequest request = new DeleteHearingRequest();
        HearingResponse response = new HearingResponse();

        when(hmcHearingApi.deleteHearing(AUTH_HEADER, SERVICE_AUTH_HEADER, DEPLOYMENT_ID, DATA_STORE_URL,
            ROLE_ASSIGNMENT_URL, HEARING_ID, request)).thenReturn(response);

        HearingResponse result = hmcHearingService.deleteHearing(HEARING_ID, request);

        assertThat(result).isNotNull().isEqualTo(response);
        verify(hmcHearingApi).deleteHearing(AUTH_HEADER, SERVICE_AUTH_HEADER, DEPLOYMENT_ID,
                                            DATA_STORE_URL, ROLE_ASSIGNMENT_URL, HEARING_ID, request);
    }

    @Test
    void shouldGetHearing() {
        GetHearingsResponse response = new GetHearingsResponse();

        when(hmcHearingApi.getHearing(AUTH_HEADER, SERVICE_AUTH_HEADER, DEPLOYMENT_ID, DATA_STORE_URL,
             ROLE_ASSIGNMENT_URL, HEARING_ID, null)).thenReturn(response);

        GetHearingsResponse result = hmcHearingService.getHearing(HEARING_ID);

        assertThat(result).isNotNull().isEqualTo(response);
        verify(hmcHearingApi).getHearing(AUTH_HEADER, SERVICE_AUTH_HEADER, DEPLOYMENT_ID,
                                         DATA_STORE_URL, ROLE_ASSIGNMENT_URL, HEARING_ID, null);
    }
}
