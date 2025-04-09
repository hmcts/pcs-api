package uk.gov.hmcts.reform.pcs.postcode.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class S2AuthTokenService {

    public static final String CIVIL_SERVICE = "civil_service";
    private final WebClient webClient;

    public S2AuthTokenService(@Value("${idam.api.url}") String baseUrl) {
        this.webClient = WebClient.builder()
//            .baseUrl("http://rpe-service-auth-provider-aat.service.core-compute-aat.internal")
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    public String getServiceToken() {
        return webClient.post()
            .uri("/testing-support/lease")
            .bodyValue("{ \"microservice\": \"" + CIVIL_SERVICE + "\" }")
            .retrieve()
            .bodyToMono(String.class).block();
    }
}
