package uk.gov.hmcts.reform.pcs.postcodecourt.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import uk.gov.hmcts.reform.pcs.Application;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.postcodecourt.entity.PostCodeCourtEntity;
import uk.gov.hmcts.reform.pcs.postcodecourt.repository.PostCodeCourtRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static uk.gov.hmcts.reform.pcs.postcodecourt.controller.PostCodeCourtController.COURTS_ENDPOINT;
import static uk.gov.hmcts.reform.pcs.postcodecourt.controller.PostCodeCourtController.POSTCODE;

@Slf4j
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
class PostCodeCourtEntityControllerIT extends AbstractPostgresContainerIT {

    private static final String AUTH_HEADER = "Bearer token";
    private static final String SERVICE_AUTH_HEADER = "ServiceAuthToken";
    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    @Autowired
    private WebTestClient webTestClient;
    @Autowired
    private PostCodeCourtRepository postCodeCourtRepository;

    @Test
    @DisplayName("Should return valid Http OK for known postcodes. The response should be empty.")
    void shouldReturnValidHttpOKForKnownPostCodes() {
        // Given
        List<PostCodeCourtEntity> all = postCodeCourtRepository.findAll();

        // When && Then
        assertThat(all).isNotEmpty();
        all.forEach(postCodeCourtEntity -> {
            webTestClient.get()
                            .uri(uriBuilder -> uriBuilder.path(COURTS_ENDPOINT)
                            .queryParam(POSTCODE, postCodeCourtEntity.getId().getPostCode()).build())
                    .header(AUTHORIZATION, AUTH_HEADER)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                    .exchange().expectStatus().isOk().expectBody().isEmpty();
        });
    }

    @DisplayName("Should return bad request for missing service token.")
    @Test
    void shouldReturnBadRequestForMissingServiceToken() {
        // Given
        String postCode = "UB7 0DG";

        // When && Then
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(COURTS_ENDPOINT)
                        .queryParam(POSTCODE, postCode).build())
                .header(AUTHORIZATION, AUTH_HEADER)
                .exchange().expectStatus().isBadRequest();

    }

    @DisplayName("Should return bad request for missing authorization token.")
    @Test
    void shouldReturnBadRequestForMissingAuthorizationToken() throws Exception {
        // Given
        String postCode = "UB7 0DG";

        // When && Then
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(COURTS_ENDPOINT)
                        .queryParam(POSTCODE, postCode).build())
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                .exchange().expectStatus().isBadRequest();

    }

}
