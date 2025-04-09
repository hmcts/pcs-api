package uk.gov.hmcts.reform.pcs.postcode.adapter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import uk.gov.hmcts.reform.pcs.postcode.auth.S2AuthTokenService;
import uk.gov.hmcts.reform.pcs.postcode.auth.IdamAuthTokenService;
import uk.gov.hmcts.reform.pcs.postcode.record.CourtVenue;
import uk.gov.hmcts.reform.pcs.postcode.record.CourtVenueResponse;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LocationReferenceDataAdapterTest {


    @Mock
    private S2AuthTokenService s2AuthTokenService;

    @Mock
    private IdamAuthTokenService idamAuthTokenService;

    @Mock
    private WebClient.Builder webClientBuilder;


    @Mock
    private WebClient webClientMock;

    @SuppressWarnings("rawtypes")
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpecMock;

    @SuppressWarnings("rawtypes")
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpecMock;


    @Mock
    private WebClient.ResponseSpec responseSpecMock;

    private LocationReferenceDataAdapterImpl locationReferenceDataAdapter;

    @BeforeEach
    void setUp() {
        String baseUrl = "https://rd-location-ref-api-aat:service:core-compute-aat:";
        when(webClientBuilder.baseUrl(baseUrl)).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClientMock);
        locationReferenceDataAdapter = new LocationReferenceDataAdapterImpl(webClientBuilder, baseUrl, s2AuthTokenService, idamAuthTokenService);
    }

    @SuppressWarnings("unchecked")
    @Test
    void givenEpimmsId_shouldFetchCountyCourtsList() {
        // Given
        int epimmsId = 1234;
        CourtVenueResponse response1 = new CourtVenueResponse(1, "Court A");
        CourtVenueResponse response2 = new CourtVenueResponse(2, "Court B");

        when(webClientMock.get()).thenReturn(requestHeadersUriSpecMock);
        when(requestHeadersUriSpecMock.uri(any(Function.class))).thenReturn(requestHeadersSpecMock);
        when(requestHeadersSpecMock.retrieve()).thenReturn(responseSpecMock);
        when(responseSpecMock.bodyToFlux(CourtVenueResponse.class)).thenReturn(Flux.just(response1, response2));

        // When
        List<CourtVenue> result = locationReferenceDataAdapter.fetchCountyCourts(epimmsId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Court A", result.get(0).courtName());
        assertEquals(1, result.get(0).courtVenueId());
        assertEquals("Court B", result.get(1).courtName());
    }
}
