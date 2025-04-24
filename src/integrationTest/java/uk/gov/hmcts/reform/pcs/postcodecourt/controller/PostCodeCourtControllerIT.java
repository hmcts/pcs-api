package uk.gov.hmcts.reform.pcs.postcodecourt.controller;

import com.azure.core.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.Application;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.location.service.api.LocationReferenceApi;
import uk.gov.hmcts.reform.pcs.postcodecourt.entity.PostCodeCourtEntity;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.Court;
import uk.gov.hmcts.reform.pcs.postcodecourt.repository.PostCodeCourtRepository;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static uk.gov.hmcts.reform.pcs.postcodecourt.controller.PostCodeCourtController.COURTS_ENDPOINT;
import static uk.gov.hmcts.reform.pcs.postcodecourt.controller.PostCodeCourtController.POSTCODE;

@Slf4j
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
class PostCodeCourtControllerIT extends AbstractPostgresContainerIT {

    private static final String AUTH_HEADER = "Bearer token";
    private static final String PCS_SERVICE_AUTH_HEADER = "Bearer serviceToken";
    private static final String LOC_REF_SERVICE_AUTH_HEADER = "Bearer locServiceToken";
    private static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    private static final int COUNTY_COURT_TYPE_ID = 10;

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private PostCodeCourtRepository postCodeCourtRepository;

    @MockitoBean
    private AuthTokenGenerator authTokenGenerator;

    @MockitoBean
    private LocationReferenceApi locationReferenceApi;

    @Test
    @DisplayName("Should return valid Http OK for known postcodes. The response should be empty for all epimIds.")
    void shouldReturnValidHttpOKForKnownPostCodes() {
        List<PostCodeCourtEntity> all = postCodeCourtRepository.findAll();
        when(authTokenGenerator.generate()).thenReturn(LOC_REF_SERVICE_AUTH_HEADER);

        all.forEach(postCodeCourtEntity -> when(locationReferenceApi.getCountyCourts(
                AUTH_HEADER,
                LOC_REF_SERVICE_AUTH_HEADER,
                postCodeCourtEntity.getId().getEpimId().toString(),
                COUNTY_COURT_TYPE_ID
        )).thenReturn(Collections.emptyList()));
        assertThat(all).isNotEmpty();
        assertingResponseToBeEmptyList(all);
    }

    @Test
    @DisplayName("Should return valid Http OK and non-empty response body for known postcodes")
    void shouldReturnHttpOkAndNonEmptyResponseForKnownPostCodes() {
        List<PostCodeCourtEntity> postCodeCourtEntities = postCodeCourtRepository.findAll();
        assertThat(postCodeCourtEntities)
                .isNotEmpty();

        Map<String, Court> expectedCourtVenues = Map.of(
            "W3 7RX", new Court(40827, "Central London County Court", 20262),
            "W3 6RS", new Court(40838, "Brentford County Court And Family Court", 36791),
            "M13 9PL", new Court(40896, "Manchester Crown Court", 144641)
        );

        when(authTokenGenerator.generate()).thenReturn(LOC_REF_SERVICE_AUTH_HEADER);

        postCodeCourtEntities.forEach(entity -> {
            String postCode = entity.getId().getPostCode();
            String epimId = entity.getId().getEpimId().toString();

            Court court = expectedCourtVenues.get(postCode);
            List<CourtVenue> mockResponse = (court != null)
                    ? List.of(new CourtVenue(entity.getId().getEpimId(), court.id(), court.name()))
                    : Collections.emptyList();

            when(locationReferenceApi.getCountyCourts(
                    AUTH_HEADER,
                    LOC_REF_SERVICE_AUTH_HEADER,
                    epimId,
                    COUNTY_COURT_TYPE_ID
            )).thenReturn(mockResponse);
        });

        postCodeCourtEntities.forEach(entity -> {
            String postCode = entity.getId().getPostCode();

            webTestClient.get()
                    .uri(uriBuilder -> uriBuilder.path(COURTS_ENDPOINT)
                            .queryParam(POSTCODE, postCode).build())
                    .header(AUTHORIZATION, AUTH_HEADER)
                    .header(SERVICE_AUTHORIZATION, PCS_SERVICE_AUTH_HEADER)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBodyList(Court.class)
                    .consumeWith(response -> {
                        List<Court> body = response.getResponseBody();
                        assertThat(body).isNotNull();

                        if (expectedCourtVenues.containsKey(postCode)) {
                            assertThat(body).isNotEmpty();
                            Court actual = body.getFirst();
                            Court expected = expectedCourtVenues.get(postCode);

                            assertThat(actual.epimId()).isEqualTo(expected.epimId());
                            assertThat(actual.id()).isEqualTo(expected.id());
                            assertThat(actual.name()).isEqualTo(expected.name());
                        } else {
                            assertThat(body).isEmpty();
                        }
                    });
        });
    }


    @DisplayName("Should return empty list when for given postcode's epimId doesn't have any matching county courts.")
    @Test
    void shouldThrow404NotfoundExceptionFromLocationReferenceThenReceiveEmptyListInResponse() {
        String postCode = "LE2 0QB";

        List<PostCodeCourtEntity> all = postCodeCourtRepository.findByIdPostCode(postCode);

        when(authTokenGenerator.generate()).thenReturn(LOC_REF_SERVICE_AUTH_HEADER);

        all.forEach(postCodeCourtEntity -> when(locationReferenceApi.getCountyCourts(
                AUTH_HEADER,
                LOC_REF_SERVICE_AUTH_HEADER,
                postCodeCourtEntity.getId().getEpimId().toString(),
                COUNTY_COURT_TYPE_ID
        )).thenThrow(new ResourceNotFoundException("No matching courts found for ".concat(postCode), null)));

        assertingResponseToBeEmptyList(all);
    }

    @DisplayName("Should return bad request for missing service token.")
    @Test
    void shouldReturnBadRequestForMissingServiceToken() {
        String postCode = "UB7 0DG";

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(COURTS_ENDPOINT)
                        .queryParam(POSTCODE, postCode).build())
                .header(AUTHORIZATION, AUTH_HEADER)
                .exchange().expectStatus().isBadRequest();

    }

    @DisplayName("Should return bad request for missing authorization token.")
    @Test
    void shouldReturnBadRequestForMissingAuthorizationToken() {
        String postCode = "UB7 0DG";

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(COURTS_ENDPOINT)
                        .queryParam(POSTCODE, postCode).build())
                .header(SERVICE_AUTHORIZATION, PCS_SERVICE_AUTH_HEADER)
                .exchange().expectStatus().isBadRequest();

    }

    private void assertingResponseToBeEmptyList(List<PostCodeCourtEntity> all) {
        all.forEach(postCodeCourtEntity -> webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(COURTS_ENDPOINT)
                        .queryParam(POSTCODE, postCodeCourtEntity.getId().getPostCode()).build())
                .header(AUTHORIZATION, AUTH_HEADER)
                .header(SERVICE_AUTHORIZATION, PCS_SERVICE_AUTH_HEADER)
                .exchange()
                .expectStatus().isOk()
                .expectBody(List.class)
                .consumeWith(response -> {
                    List<?> body = response.getResponseBody();
                    assert body != null;
                    assert body.isEmpty();
                }));
    }
}
