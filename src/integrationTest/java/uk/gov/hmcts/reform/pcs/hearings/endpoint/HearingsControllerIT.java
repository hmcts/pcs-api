package uk.gov.hmcts.reform.pcs.hearings.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.config.IntegrationTest;
import uk.gov.hmcts.reform.pcs.hearings.model.DeleteHearingRequest;
import uk.gov.hmcts.reform.pcs.hearings.model.HearingRequest;
import uk.gov.hmcts.reform.pcs.hearings.model.UpdateHearingRequest;
import uk.gov.hmcts.reform.pcs.hearings.service.api.HmcHearingApi;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.pcs.hearings.constants.HearingConstants.SERVICE_AUTHORIZATION;

@IntegrationTest
class HearingsControllerIT extends AbstractPostgresContainerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthTokenGenerator authTokenGenerator;

    @MockitoBean
    private HmcHearingApi hmcHearingApi;

    private static final String AUTH_HEADER = "Bearer token";
    private static final String SERVICE_AUTH_HEADER = "ServiceAuthToken";

    @Test
    void shouldCreateHearing() throws Exception {
        HearingRequest request = new HearingRequest();
        mockMvc.perform(post("/hearing/")
                            .header(AUTHORIZATION, AUTH_HEADER)
                            .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());
    }

    @Test
    void shouldUpdateHearing() throws Exception {
        UpdateHearingRequest request = new UpdateHearingRequest();
        mockMvc.perform(put("/hearing/123")
                            .header(AUTHORIZATION, AUTH_HEADER)
                            .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

    }

    @Test
    void shouldDeleteHearing() throws Exception {
        DeleteHearingRequest request = new DeleteHearingRequest();
        mockMvc.perform(delete("/hearing/123")
                            .header(AUTHORIZATION, AUTH_HEADER)
                            .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());
    }

    @Test
    void shouldGetHearing() throws Exception {
        mockMvc.perform(get("/hearing/123")
                            .header(AUTHORIZATION, AUTH_HEADER)
                            .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER))
            .andExpect(status().isOk());
    }
}
