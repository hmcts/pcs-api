package uk.gov.hmcts.reform.pcs.postcodecourt.controller;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.pcs.audit.Audit;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.postcodecourt.domain.PostCodeCourt;
import uk.gov.hmcts.reform.pcs.postcodecourt.repository.PostCodeCourtRepository;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@AutoConfigureMockMvc
class PostCodeCourtControllerIT extends AbstractPostgresContainerIT {

    private static final String AUTH_HEADER = "Bearer token";
    private static final String SERVICE_AUTH_HEADER = "ServiceAuthToken";
    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    public static final String COURT = "/court";
    public static final String POSTCODE = "postCode";

    @Autowired
    private transient MockMvc mockMvc;
    @Autowired
    private PostCodeCourtRepository postCodeCourtRepository;

    @DisplayName("Should return valid Http 200 response code from a known postcode.")
    @Test
    @FlywayTest(invokeCleanDB = false, invokeMigrateDB = false, invokeBaselineDB = true)
    void shouldReturnValidResponseForAKnownPostcode() throws Exception {
        // Given
        //flyway.migrate();
        String postCode = "W3 7RX";
        int epimId = 20262;
        postCodeCourtRepository.save(newPostCode(postCode, epimId));

        // When
        final MockHttpServletResponse response = mockMvc.perform(get(COURT)
                                                                  .header(AUTHORIZATION, AUTH_HEADER)
                                                                  .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                                                                  .queryParam(POSTCODE, postCode))
            .andReturn().getResponse();
        // Then
        assertThat(response.getStatus()).isEqualTo(OK.value());
        assertThat(response.getContentLength()).isZero();
    }

    @DisplayName("Should return Bad Request response for empty postcode.")
    @Test
    @FlywayTest
    void shouldReturnBadRequestForInvalidServiceToken() throws Exception {
        // Given
        String postCode = "";

        // When
        final MockHttpServletResponse response = mockMvc.perform(get(COURT)
                                                                     .header(AUTHORIZATION, AUTH_HEADER)
                                                                     .header(SERVICE_AUTHORIZATION, "")
                                                                     .queryParam(POSTCODE, postCode))
            .andReturn().getResponse();

        // Then
        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST.value());
        assertThat(response.getContentLength()).isZero();
    }

    private PostCodeCourt newPostCode(String postCode, int epimId) {
        PostCodeCourt postCodeCourt = new PostCodeCourt();
        postCodeCourt.setPostCode(postCode);
        postCodeCourt.setEpimId(epimId);
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
