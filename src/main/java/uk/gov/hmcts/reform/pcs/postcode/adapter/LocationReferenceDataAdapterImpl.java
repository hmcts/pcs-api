package uk.gov.hmcts.reform.pcs.postcode.adapter;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.hmcts.reform.pcs.postcode.record.CourtVenue;
import uk.gov.hmcts.reform.pcs.postcode.record.CourtVenueResponse;

import java.util.List;

@Service
public class LocationReferenceDataAdapterImpl implements LocationReferenceDataAdapter {

    public static final int COUNTY_COURT_TYPE_ID = 10;
    private final WebClient webClient;

    public LocationReferenceDataAdapterImpl(WebClient.Builder webClientBuilder,
                                            @Value("${location-reference.service.url}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public List<CourtVenue> fetchCountyCourts(int epimmsId) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("internal/refdata/location/court-venues")
                .queryParam("epimms_id", epimmsId)
                .queryParam("court_type_id", COUNTY_COURT_TYPE_ID)
                .build())
            .retrieve()
            .bodyToFlux(CourtVenueResponse.class)
            .map(res -> new CourtVenue(res.courtVenueId(), res.courtName()))
            .collectList()
            .block();  // Blocking version
    }
}
