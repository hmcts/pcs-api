package uk.gov.hmcts.reform.pcs.postcodecourt.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.pcs.audit.Audit;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.config.IntegrationTest;
import uk.gov.hmcts.reform.pcs.postcodecourt.domain.PostCodeCourt;
import uk.gov.hmcts.reform.pcs.postcodecourt.domain.PostCodeCourtKey;
import uk.gov.hmcts.reform.pcs.postcodecourt.repository.PostCodeCourtRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static uk.gov.hmcts.reform.pcs.postcodecourt.controller.PostCodeCourtController.COURTS_ENDPOINT;
import static uk.gov.hmcts.reform.pcs.postcodecourt.controller.PostCodeCourtController.POSTCODE;

@Slf4j
@IntegrationTest
class PostCodeCourtControllerIT extends AbstractPostgresContainerIT {

    private static final String AUTH_HEADER = "Bearer token";
    private static final String SERVICE_AUTH_HEADER = "ServiceAuthToken";
    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    @Autowired
    private transient MockMvc mockMvc;
    @Autowired
    private PostCodeCourtRepository postCodeCourtRepository;

    @Test
    @DisplayName("Should return valid Http OK for known postcodes. The response should be empty.")
    void shouldReturnValidHttpOKForKnownPostCodes() {
        // Given
        List<PostCodeCourt> all = postCodeCourtRepository.findAll();

        // When
        all.forEach(postCodeCourt -> {
            try {
                MockHttpServletResponse response = mockMvc.perform(get(COURTS_ENDPOINT)
                                .header(AUTHORIZATION, AUTH_HEADER)
                                .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                                .queryParam(POSTCODE, postCodeCourt.getId().getPostCode()))
                        .andReturn().getResponse();

                // Then
                assertThat(response.getStatus()).isEqualTo(OK.value());
                assertThat(response.getContentLength()).isZero();
            } catch (Exception e) {
                fail("Unable to find postcode: " + postCodeCourt.getId().getPostCode());
            }
        });

    }

    @DisplayName("Should return valid Http 200 response code from a known postcode.")
    @Test
    void shouldReturnValidResponseForAKnownPostCode() throws Exception {
        // Given
        String postCode = "W3 7RX";
        int epimId = 20262;
        postCodeCourtRepository.save(createPostCodeCourt(postCode, epimId));

        // When
        MockHttpServletResponse response = mockMvc.perform(get(COURTS_ENDPOINT)
                                                                  .header(AUTHORIZATION, AUTH_HEADER)
                                                                  .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                                                                  .queryParam(POSTCODE, postCode))
            .andReturn().getResponse();

        // Then
        assertThat(response.getStatus()).isEqualTo(OK.value());
        assertThat(response.getContentLength()).isZero();
    }

    @DisplayName("Should return bad request for missing service token.")
    @Test
    void shouldReturnBadRequestForMissingServiceToken() throws Exception {
        // Given
        String postCode = "UB7 0DG";

        // When
        MockHttpServletResponse response = mockMvc.perform(get(COURTS_ENDPOINT)
                                                                     .header(AUTHORIZATION, AUTH_HEADER)
                                                                     .queryParam(POSTCODE, postCode))
            .andReturn().getResponse();

        // Then
        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST.value());
        assertThat(response.getContentLength()).isZero();
    }

    @DisplayName("Should return bad request for missing authorization token.")
    @Test
    void shouldReturnBadRequestForMissingAuthorizationToken() throws Exception {
        // Given
        String postCode = "UB7 0DG";

        // When
        MockHttpServletResponse response = mockMvc.perform(get(COURTS_ENDPOINT)
                        .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                        .queryParam(POSTCODE, postCode))
                .andReturn().getResponse();

        // Then
        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST.value());
        assertThat(response.getContentLength()).isZero();
    }

    private PostCodeCourt createPostCodeCourt(String postCode, int epimId) {
        PostCodeCourt postCodeCourt = new PostCodeCourt();
        postCodeCourt.setId(new PostCodeCourtKey(postCode, epimId));
        populateRemaining(postCodeCourt);
        return postCodeCourt;
    }

    private void populateRemaining(PostCodeCourt postCodeCourt) {
        postCodeCourt.setEffectiveFrom(LocalDateTime.now());
        postCodeCourt.setEffectiveTo(LocalDateTime.now().plusMonths(1));
        postCodeCourt.setLegislativeCountry("England");
        postCodeCourt.setAudit(new Audit());
    }

}
