package uk.gov.hmcts.reform.pcs.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.TokenResponse;

import static org.mockito.Mockito.when;

@Component
public class IdamHelper {

    @Value("${idam.system-user.username}")
    private String pcsSystemUser;

    @Value("${idam.system-user.password}")
    private String pcsSystemPassword;

    public void stubIdamSystemUser(IdamClient idamClient, String idToken) {
        TokenResponse tokenResponse = new TokenResponse("some access token", "expires",
                                                        idToken, "some refresh token", "some scope",
                                                        "some token type");

        when(idamClient.getAccessTokenResponse(pcsSystemUser, pcsSystemPassword))
            .thenReturn(tokenResponse);
    }
}
