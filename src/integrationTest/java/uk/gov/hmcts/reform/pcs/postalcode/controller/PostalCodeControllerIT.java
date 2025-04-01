package uk.gov.hmcts.reform.pcs.postalcode.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.pcs.audit.Audit;
import uk.gov.hmcts.reform.pcs.config.AbstractIT;
import uk.gov.hmcts.reform.pcs.postalcode.domain.PostCode;
import uk.gov.hmcts.reform.pcs.postalcode.dto.PostCodeResponse;
import uk.gov.hmcts.reform.pcs.postalcode.repository.PostalCodeRepository;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@AutoConfigureMockMvc
class PostalCodeControllerIT extends AbstractIT {

    private static final String AUTH_HEADER = "Bearer token";
    private static final String SERVICE_AUTH_HEADER = "ServiceAuthToken";
    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    public static final String COURT = "/court";
    public static final String POSTCODE = "postcode";

    @Autowired
    private transient MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PostalCodeRepository postalCodeRepository;

    @DisplayName("Should return valid PostCodeResponse with 200 response code")
    @Test
    void returnValidResponseWithEpimIdFromKnownPostcode() throws Exception {
        // Given
        String postCode = "W3 7RX";
        int epimId = 20262;
        postalCodeRepository.save(newPostCode(postCode, epimId));

        // When
        final MockHttpServletResponse response = mockMvc.perform(get(COURT)
                                                                  .header(AUTHORIZATION, AUTH_HEADER)
                                                                  .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                                                                  .queryParam(POSTCODE, postCode))
            .andReturn().getResponse();

        // Then
        assertThat(response.getStatus()).isEqualTo(OK.value());
        final PostCodeResponse postCodeResponse = getPostCodeResponse(response);
        assertThat(postCodeResponse.getEpimId()).isEqualTo(epimId);
    }

    @SneakyThrows
    private PostCodeResponse getPostCodeResponse(MockHttpServletResponse response) {
        return objectMapper.readValue(response.getContentAsString(), PostCodeResponse.class);
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
        final PostCodeResponse postCodeResponse = getPostCodeResponse(response);
        assertThat(postCodeResponse.getEpimId()).isZero();
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

    private PostCode newPostCode(String postCodeValue, int epimId) {
        PostCode postCode = new PostCode();
        postCode.setPostCode(postCodeValue);
        postCode.setEpimId(epimId);
        populateRemaining(postCode);
        return postCode;
    }

    private void populateRemaining(PostCode postCode) {
        postCode.setEffectiveFrom(LocalDateTime.now());
        postCode.setEffectiveTo(LocalDateTime.now().plusMonths(1));
        postCode.setLegislativeCountry("England");
        postCode.setAudit(new Audit());
    }
}
