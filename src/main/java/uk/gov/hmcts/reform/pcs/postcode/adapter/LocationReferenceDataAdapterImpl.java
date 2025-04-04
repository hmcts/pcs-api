package uk.gov.hmcts.reform.pcs.postcode.adapter;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.hmcts.reform.pcs.postcode.record.CourtVenueResponse;

@Service
public class LocationReferenceDataAdapterImpl implements LocationReferenceDataAdapter {

    private final WebClient webClient;

    public LocationReferenceDataAdapterImpl(WebClient.Builder webClientBuilder,
                                            @Value("${location-reference.service.url}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public String fetchCourtName(int epimId) {
        return webClient.get()
            .uri("/refdata/location/court-venues/" + epimId)
            .retrieve()
            .bodyToFlux(CourtVenueResponse.class)
            .next()
            .map(CourtVenueResponse::courtName).block();
    }
}
