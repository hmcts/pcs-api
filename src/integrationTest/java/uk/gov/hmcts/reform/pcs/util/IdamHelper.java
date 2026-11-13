package uk.gov.hmcts.reform.pcs.util;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.TokenResponse;
import uk.gov.hmcts.reform.pcs.idam.User;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Component
public class IdamHelper {

    public void stubIdamSystemUser(IdamClient idamClient, String accessToken) {
        when(idamClient.getAccessTokenResponse(anyString(), anyString()))
            .thenReturn(new TokenResponse(accessToken, "3600", null, null, "openid profile roles", "Bearer"));
    }

    public UsernamePasswordAuthenticationToken setUpAuthenticatedUser(
        IdamClient idamClient, String systemUserId, UUID userId) {
        stubIdamSystemUser(idamClient, systemUserId);
        uk.gov.hmcts.reform.idam.client.models.UserInfo idamUserInfo =
            mock(uk.gov.hmcts.reform.idam.client.models.UserInfo.class);
        when(idamUserInfo.getUid()).thenReturn(userId.toString());
        when(idamClient.getUserInfo(anyString())).thenReturn(idamUserInfo);
        UserInfo userInfo = UserInfo.builder().uid(userId.toString()).build();
        User user = new User("testing", userInfo);
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
        return auth;
    }

}
