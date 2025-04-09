package uk.gov.hmcts.reform.pcs.postcode.auth;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "idam.token")
@Getter
@Setter
public class IdamTokenProperties {
    private String url;
    private String username;
    private String password;
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String scope;
    private String grantType;
}
