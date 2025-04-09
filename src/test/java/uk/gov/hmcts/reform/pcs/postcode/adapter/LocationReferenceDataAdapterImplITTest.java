package uk.gov.hmcts.reform.pcs.postcode.adapter;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.hmcts.reform.pcs.postcode.auth.IdamAuthTokenService;
import uk.gov.hmcts.reform.pcs.postcode.auth.IdamTokenProperties;
import uk.gov.hmcts.reform.pcs.postcode.auth.S2AuthTokenService;

import static org.mockito.Mockito.when;

public class LocationReferenceDataAdapterImplITTest {

    private S2AuthTokenService s2AuthTokenService;
    private IdamAuthTokenService idamAuthTokenService;

    private LocationReferenceDataAdapterImpl locationReferenceDataAdapter;

    @BeforeEach
    void setUp() {
        s2AuthTokenService = new S2AuthTokenService("http://rpe-service-auth-provider-aat.service.core-compute-aat.internal");
        idamAuthTokenService = new IdamAuthTokenService(getIdamTokenProperties());
        String baseUrl = "https://rd-location-ref-api-aat.service:core-compute-aat";

        WebClient.Builder webClientBuilder = WebClient.builder();
        locationReferenceDataAdapter = new LocationReferenceDataAdapterImpl(webClientBuilder, baseUrl, s2AuthTokenService, idamAuthTokenService);
    }

    private static @NotNull IdamTokenProperties getIdamTokenProperties() {
        IdamTokenProperties idamTokenProperties = new IdamTokenProperties();
        idamTokenProperties.setUsername("civil-system-update@mailnesia.com");
        idamTokenProperties.setPassword("Password12");
        idamTokenProperties.setClientId("civil_citizen_ui");
        idamTokenProperties.setClientSecret("47js6e86Wv5718D2O77OL466020731ii");
        idamTokenProperties.setRedirectUri("https://civil-citizen-ui.aat.platform.hmcts.net/oauth2/callback");
        idamTokenProperties.setGrantType("password");
        idamTokenProperties.setScope("profile openid roles");
        return idamTokenProperties;
    }


    @Test
    void givenEpimmsId_shouldFetchCountyCourtsList() {
        locationReferenceDataAdapter.fetchCountyCourts(20626);

    }
}
