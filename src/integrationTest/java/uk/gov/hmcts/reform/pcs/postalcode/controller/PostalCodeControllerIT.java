package uk.gov.hmcts.reform.pcs.postalcode.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.pcs.postalcode.dto.PostCodeResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
class PostalCodeControllerIT {

    private static final String AUTH_HEADER = "Bearer token";
    private static final String SERVICE_AUTH_HEADER = "ServiceAuthToken";
    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    public static final String COURT = "/court";
    public static final String POSTCODE = "postcode";

    @Autowired
    private transient MockMvc mockMvc;

    private final ObjectMapper objectMapper;

    @Autowired
    public PostalCodeControllerIT(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @DisplayName("Should return valid PostCodeResponse with 200 response code")
    @Test
    void returnValidResponseWithEpimsIdFromKnownPostcode() throws Exception {
        // Given
        String postcode = "W3 7RX";
        int epimsId = 20262;

        // When
        final MockHttpServletResponse response = mockMvc.perform(get(COURT)
                                                                  .header(AUTHORIZATION, AUTH_HEADER)
                                                                  .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                                                                  .queryParam(POSTCODE, postcode))
            .andReturn().getResponse();

        // Then
        assertThat(response.getStatus()).isEqualTo(OK.value());
        final PostCodeResponse postCodeResponse = objectMapper
            .readValue(response.getContentAsString(), PostCodeResponse.class);
        assertThat(postCodeResponse.getEPIMSId()).isEqualTo(epimsId);
    }

    @DisplayName("Should return empty PostCodeResponse with 200 response code for an unknown postcode.")
    @Test
    void returnEmptyResponseFromUnknownPostcode() throws Exception {
        // Given
        String postcode = "MK1 7RC";

        // When
        final MockHttpServletResponse response = mockMvc.perform(get(COURT)
                                                                     .header(AUTHORIZATION, AUTH_HEADER)
                                                                     .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                                                                     .queryParam(POSTCODE, postcode))
            .andReturn().getResponse();

        // Then
        assertThat(response.getStatus()).isEqualTo(OK.value());
        final PostCodeResponse postCodeResponse = objectMapper
            .readValue(response.getContentAsString(), PostCodeResponse.class);
        assertThat(postCodeResponse.getEPIMSId()).isZero();
    }

    @DisplayName("Should return Bad Request response for empty postcode.")
    @Test
    void returnEmptyResponseFromEmptyPostcodeWithBadRequestCode() throws Exception {
        // Given
        String postcode = "";

        // When
        final MockHttpServletResponse response = mockMvc.perform(get(COURT)
                                                                     .header(AUTHORIZATION, AUTH_HEADER)
                                                                     .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                                                                     .queryParam(POSTCODE, postcode))
            .andReturn().getResponse();

        // Then
        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST.value());
        assertThat(response.getContentLength()).isZero();
    }

}
