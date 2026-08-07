package uk.gov.hmcts.reform.pcs.ccd.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CaseAssignmentApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRole;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRolesResource;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserRoleService {

    private static final Duration RAS_ROLE_CACHE_TTL = Duration.ofMinutes(1);

    private final SecurityContextService securityContextService;
    private final AuthTokenGenerator authTokenGenerator;
    private final CaseAssignmentApi caseAssignmentApi;
    private final Cache<CacheKey, Set<String>> rasRoleCache;

    public UserRoleService(SecurityContextService securityContextService,
                           AuthTokenGenerator authTokenGenerator,
                           CaseAssignmentApi caseAssignmentApi) {
        this.securityContextService = securityContextService;
        this.authTokenGenerator = authTokenGenerator;
        this.caseAssignmentApi = caseAssignmentApi;
        this.rasRoleCache = Caffeine.newBuilder()
            .expireAfterWrite(RAS_ROLE_CACHE_TTL)
            .build();
    }

    public UserRoles getCurrentUserCaseRoles(long caseReference) {
        UserInfo currentUserDetails = securityContextService.getCurrentUserDetails();
        String currentUserId = currentUserDetails.getUid();

        Set<String> roles = new LinkedHashSet<>(safeRoles(currentUserDetails.getRoles()));
        roles.addAll(rasRoleCache.get(
            new CacheKey(caseReference, currentUserId),
            this::getRasRoles
        ));

        return new UserRoles(UUID.fromString(currentUserId), List.copyOf(roles));
    }

    private Set<String> getRasRoles(CacheKey cacheKey) {
        CaseAssignmentUserRolesResource userRoles = caseAssignmentApi.getUserRoles(
            securityContextService.getCurrentUserAuthToken(),
            authTokenGenerator.generate(),
            List.of(String.valueOf(cacheKey.caseReference())),
            List.of(cacheKey.userId())
        );

        if (userRoles == null || userRoles.getCaseAssignmentUserRoles() == null) {
            return Set.of();
        }

        return userRoles.getCaseAssignmentUserRoles().stream()
            .map(CaseAssignmentUserRole::getCaseRole)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Collection<String> safeRoles(Collection<String> roles) {
        return roles != null ? roles : List.of();
    }

    private record CacheKey(long caseReference, String userId) {
    }
}
