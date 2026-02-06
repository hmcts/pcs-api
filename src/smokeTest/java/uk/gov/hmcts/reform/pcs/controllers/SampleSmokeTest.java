package uk.gov.hmcts.reform.pcs.controllers;

import lombok.Getter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SampleSmokeTest {

    private static final String BASE_URL = System.getenv().getOrDefault("TEST_URL", "http://localhost:8080");
    private static final String S2S_URL = System.getenv().getOrDefault("IDAM_S2S_AUTH_URL", "http://localhost:4502");

    private final RestTemplate restTemplate = new RestTemplate();

    private String s2sToken;


    @BeforeEach
    public void setUp() {
        s2sToken = getS2sToken(restTemplate);
    }

    @Test
    void smokeTest() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set(ServiceAuthFilter.AUTHORISATION, s2sToken);

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<Greeting> response = restTemplate.exchange(
            BASE_URL, HttpMethod.GET, requestEntity, Greeting.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getMessage()).startsWith("Welcome");
    }

    private String getS2sToken(RestTemplate restTemplate) {
        return restTemplate
            .postForObject(
                S2S_URL + "/testing-support/lease",
                Map.of("microservice", "pcs-api"),
                String.class
            );
    }

    @Getter
    @SuppressWarnings("unused")
    private static class Greeting {
        private String message;
    }

}
