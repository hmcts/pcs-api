package uk.gov.hmcts.reform.pcs.postcodecourt.controller;

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
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;
import uk.gov.hmcts.reform.pcs.location.service.api.LocationReferenceApi;
import uk.gov.hmcts.reform.pcs.postcodecourt.entity.PostCodeCourtEntity;
import uk.gov.hmcts.reform.pcs.postcodecourt.entity.PostCodeCourtKey;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.Court;
import uk.gov.hmcts.reform.pcs.postcodecourt.repository.PostCodeCourtRepository;

import java.util.Collections;
import java.util.List;

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
    @DisplayName("Should return valid Http OK and correct response body for known postcodes")
    void shouldReturnHttpOkAndCorrectResponseForKnownPostCodes() {
        String postCode1 = "W3 7RX";
        String postCode2 = "W3 6RS";
        String postCode3 = "M13 9PL";

        final PostCodeCourtKey id1 = new PostCodeCourtKey(postCode1, 20262);
        final PostCodeCourtKey id2 = new PostCodeCourtKey(postCode2, 36791);
        final PostCodeCourtKey id3 = new PostCodeCourtKey(postCode3, 144641);

        final Court court1 = new Court(40827, "Central London County Court", id1.getEpimId());
        final Court court2 = new Court(40838, "Brentford County Court And Family Court", id2.getEpimId());
        final Court court3 = new Court(40896, "Manchester Crown Court", id3.getEpimId());

        when(authTokenGenerator.generate()).thenReturn(LOC_REF_SERVICE_AUTH_HEADER);

        stubLocationReferenceApi(id1.getEpimId().toString(),
                List.of(new CourtVenue(id1.getEpimId(), court1.id(), court1.name())));
        stubLocationReferenceApi(id2.getEpimId().toString(),
                List.of(new CourtVenue(id2.getEpimId(), court2.id(), court2.name())));
        stubLocationReferenceApi(id3.getEpimId().toString(),
                List.of(new CourtVenue(id3.getEpimId(), court3.id(), court3.name())));
        stubLocationReferenceApi("990000", Collections.emptyList());

        assertPostcodeReturns(postCode1, court1);
        assertPostcodeReturns(postCode2, court2);
        assertPostcodeReturns(postCode3, court3);
        assertPostcodeReturns("SW1H9EA", null);
    }

    private void stubLocationReferenceApi(String epimId, List<CourtVenue> response) {
        when(locationReferenceApi.getCountyCourts(
                AUTH_HEADER,
                LOC_REF_SERVICE_AUTH_HEADER,
                epimId,
                COUNTY_COURT_TYPE_ID
        )).thenReturn(response);
    }

    private void assertPostcodeReturns(String postCode, Court expectedCourt) {
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
                    if (expectedCourt == null) {
                        assertThat(body).isEmpty();
                    } else {
                        assertThat(body).isNotEmpty();
                        Court actual = body.getFirst();
                        assertThat(actual.epimId()).isEqualTo(expectedCourt.epimId());
                        assertThat(actual.id()).isEqualTo(expectedCourt.id());
                        assertThat(actual.name()).isEqualTo(expectedCourt.name());
                    }
                });
    }

    @DisplayName("Should return empty list when for given postcode's epimId doesn't have any matching county courts.")
    @Test
    void shouldThrow404NotfoundExceptionFromLocationReferenceThenReceiveEmptyListInResponse() {
        String postCode = "LE2 0QB";
        List<String> postcodes = List.of("LE2 0QB", "LE2 0Q", "LE2 0", "LE2");

        List<PostCodeCourtEntity> all = postCodeCourtRepository.findByIdPostCodeIn(postcodes);

        when(authTokenGenerator.generate()).thenReturn(LOC_REF_SERVICE_AUTH_HEADER);

        all.forEach(postCodeCourtEntity -> when(locationReferenceApi.getCountyCourts(
                AUTH_HEADER,
                LOC_REF_SERVICE_AUTH_HEADER,
                postCodeCourtEntity.getId().getEpimId().toString(),
                COUNTY_COURT_TYPE_ID
        )).thenThrow(new RuntimeException("No matching courts found for ".concat(postCode), null)));

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
                    assertThat(body)
                            .isNotNull()
                            .isEmpty();
                }));
    }
}
