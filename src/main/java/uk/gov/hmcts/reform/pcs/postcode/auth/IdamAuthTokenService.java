package uk.gov.hmcts.reform.pcs.postcode.auth;

import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Component
public class IdamAuthTokenService {

    private final WebClient webClient;
    private final IdamTokenProperties properties;

    public IdamAuthTokenService(IdamTokenProperties properties) {
        this.webClient = WebClient.builder()
            .baseUrl("https://idam-api.aat.platform.hmcts.net")
            .build();
        this.properties = properties;
    }


    public String getUserToken() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
       /* formData.add("username", "civil-system-update@mailnesia.com");
        formData.add("password", "Password12");
        formData.add("client_id", "civil_citizen_ui");
        formData.add("client_secret", "47js6e86Wv5718D2O77OL466020731ii");
        formData.add("redirect_uri", "https://civil-citizen-ui.aat.platform.hmcts.net/oauth2/callback");
        formData.add("grant_type", "password");
        formData.add("scope", "profile openid roles");*/

        formData.add("username", properties.getUsername());
        formData.add("password", properties.getPassword());
        formData.add("client_id", properties.getClientId());
        formData.add("client_secret", properties.getClientSecret());
        formData.add("redirect_uri", properties.getRedirectUri());
        formData.add("grant_type", properties.getGrantType());
        formData.add("scope", properties.getScope());

        return webClient.post()
            .uri("/o/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue(formData)
            .retrieve()
            .bodyToMono(TokenResponse.class)
            .map(TokenResponse::getAccessToken)
            .block();
    }

    @Setter
    static class TokenResponse {
        private String access_token;

        public String getAccessToken() {
            return access_token;
        }

    }
}
