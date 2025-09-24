package uk.gov.hmcts.reform.pcs.util;

import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.TokenResponse;

import static org.mockito.Mockito.when;

public class IdamHelper {

    // From application-integration.yaml
    private static final String PCS_SYSTEM_USER = "pcs-system-user-it";
    private static final String PCS_SYSTEM_PASSWORD = "pcs-system-password-it";

    public static void stubIdamSystemUser(IdamClient idamClient, String idToken) {
        TokenResponse tokenResponse = new TokenResponse("some access token", "expires",
                                                        idToken, "some refresh token",
                                                        "some scope", "some token type");

        when(idamClient.getAccessTokenResponse(PCS_SYSTEM_USER, PCS_SYSTEM_PASSWORD))
            .thenReturn(tokenResponse);
    }

}
