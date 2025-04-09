package uk.gov.hmcts.reform.pcs.postcode.adapter;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gov.hmcts.reform.pcs.postcode.auth.S2AuthTokenService;
import uk.gov.hmcts.reform.pcs.postcode.auth.IdamAuthTokenService;
import uk.gov.hmcts.reform.pcs.postcode.record.CourtVenue;
import uk.gov.hmcts.reform.pcs.postcode.record.CourtVenueResponse;

import java.util.List;

@Service
public class LocationReferenceDataAdapterImpl implements LocationReferenceDataAdapter {

    public static final int COUNTY_COURT_TYPE_ID = 10;
    private final WebClient webClient;


    private final S2AuthTokenService s2AuthTokenService;
    private final IdamAuthTokenService idamAuthTokenService;


    public LocationReferenceDataAdapterImpl(WebClient.Builder webClientBuilder,
                                            @Value("${location-reference.service.url}") String baseUrl,
                                            S2AuthTokenService s2AuthTokenService, IdamAuthTokenService idamAuthTokenService) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.s2AuthTokenService = s2AuthTokenService;
        this.idamAuthTokenService = idamAuthTokenService;
    }

    @Override
    public List<CourtVenue> fetchCountyCourts(int epimmsId) {
        String token = "";
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                     .path("internal/refdata/location/court-venues")
                     .queryParam("epimms_id", epimmsId)
                     .queryParam("court_type_id", COUNTY_COURT_TYPE_ID)
                     .build())
            .headers(httpHeaders -> {
                httpHeaders.setBearerAuth(idamAuthTokenService.getUserToken()); // sets "Authorization: Bearer <token>"
                httpHeaders.set("ServiceAuthorization", "Bearer " + s2AuthTokenService.getServiceToken()); // sets second header
            })
            .retrieve()
            .bodyToFlux(CourtVenueResponse.class)
            .map(res -> new CourtVenue(res.courtVenueId(), res.courtName()))
            .collectList()
            .block();  // Blocking version
    }

    Override
    public Mono<List<CourtVenue>> fetchCountyCourt(int epimmsId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("internal/refdata/location/court-venues")
                        .queryParam("epimms_id", epimmsId)
                        .queryParam("court_type_id", COUNTY_COURT_TYPE_ID)
                        .build())
                .retrieve()
                .bodyToFlux(CourtVenueResponse.class)
                .map(res -> new CourtVenue(res.courtVenueId(), res.courtName()))
                .collectList(); // No .block(), returns Mono<List<CourtVenue>>
    }


   /* public List<CourtVenue> fetchCountyCourts(int epimmsId) {
        String token = "";
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("internal/refdata/location/court-venues")
                        .queryParam("epimms_id", epimmsId)
                        .queryParam("court_type_id", COUNTY_COURT_TYPE_ID)
                        .build())
                .headers(httpHeaders -> {
                    httpHeaders.setBearerAuth(idamAuthTokenService.getUserToken()); // sets "Authorization: Bearer <token>"
                    httpHeaders.set("ServiceAuthorization", "Bearer " + s2AuthTokenService.getServiceToken()); // sets second header
                })
                .retrieve()
                .bodyToFlux(CourtVenueResponse.class)
                .map(res -> new CourtVenue(res.courtVenueId(), res.courtName()))
                .collectList()
                .block();  // Blocking version
    }*/


}
