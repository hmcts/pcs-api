package uk.gov.hmcts.reform.pcs.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;

@Component
public class SystemUpdateUserTokenProvider extends IdamTokenProvider {

    public SystemUpdateUserTokenProvider(OAuth2AuthorizedClientManager authorizedClientManager,
                            @Value("${idam.system-user.username}") String username,
                            @Value("${idam.system-user.password}") String password) {
        super(authorizedClientManager, "system-user", username, password);
    }
}
