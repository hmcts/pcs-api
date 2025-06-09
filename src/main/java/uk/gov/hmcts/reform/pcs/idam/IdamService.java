package uk.gov.hmcts.reform.pcs.idam;

import feign.FeignException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.TokenResponse;
import uk.gov.hmcts.reform.pcs.exception.IdamException;

@Service
public class IdamService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final IdamClient idamClient;
    private final String idamSystemUsername;
    private final String idamSystemPassword;

    public IdamService(IdamClient idamClient,
                       @Value("${idam.system-user.username}") String idamSystemUsername,
                       @Value("${idam.system-user.password}") String idamSystemPassword) {

        this.idamClient = idamClient;
        this.idamSystemUsername = idamSystemUsername;
        this.idamSystemPassword = idamSystemPassword;
    }

    public String getSystemUserAuthorisation() {
        TokenResponse accessTokenResponse = getAccessTokenResponse();
        return BEARER_PREFIX + accessTokenResponse.idToken;
    }

    private TokenResponse getAccessTokenResponse() {
        try {
            return idamClient.getAccessTokenResponse(idamSystemUsername, idamSystemPassword);
        } catch (FeignException fe) {
            throw new IdamException("Unable to get access token response", fe);
        }
    }

}
