package uk.gov.hmcts.reform.pcs.roles.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.IdamService;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import java.time.Duration;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;


@Service
public class UserInfoService {

    private static final int CACHE_MAX_SIZE = 100;
    private static final Duration CACHE_DURATION = Duration.ofSeconds(15);

    private final IdamService idamService;
    private final HttpServletRequest httpServletRequest;
    private final Cache<String, UserInfo> userInfoCache;

    public UserInfoService(IdamService idamService, HttpServletRequest httpServletRequest) {
        this.idamService = idamService;
        this.httpServletRequest = httpServletRequest;

        this.userInfoCache = Caffeine.newBuilder()
            .maximumSize(CACHE_MAX_SIZE)
            .expireAfterWrite(CACHE_DURATION)
            .build();
    }

    public UserInfo getCurrentUserInfo() {
        String authorisation = httpServletRequest.getHeader(AUTHORIZATION);
        return userInfoCache.get(authorisation, this::fetchUserInfo);
    }

    private UserInfo fetchUserInfo(String authorisation) {
        return idamService.retrieveUser(authorisation).getUserDetails();
    }

}
