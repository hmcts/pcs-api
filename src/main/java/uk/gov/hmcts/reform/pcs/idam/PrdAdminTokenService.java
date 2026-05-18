package uk.gov.hmcts.reform.pcs.idam;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.security.IdamServiceAccountTokenProvider;

@Service
public class PrdAdminTokenService extends IdamServiceAccountTokenProvider {

    public PrdAdminTokenService(OAuth2AuthorizedClientManager authorizedClientManager,
                                @Value("${idam.prd-admin.username}") String username,
                                @Value("${idam.prd-admin.password}") String password) {
        super(authorizedClientManager, "prd-admin", username, password);
    }
}
