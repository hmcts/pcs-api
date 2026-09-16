package uk.gov.hmcts.reform.pcs.ccd.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CaseAssignmentApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRole;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRolesResource;
import uk.gov.hmcts.reform.pcs.am.RoleAssignment;
import uk.gov.hmcts.reform.pcs.am.RoleAssignmentApi;
import uk.gov.hmcts.reform.pcs.am.RoleAssignmentResponse;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserRoleService {

    private static final Duration RAS_ROLE_CACHE_TTL = Duration.ofMinutes(1);

    private final SecurityContextService securityContextService;
    private final AuthTokenGenerator authTokenGenerator;
    private final CaseAssignmentApi caseAssignmentApi;
    private final RoleAssignmentApi roleAssignmentApi;
    private final Cache<CacheKey, Set<String>> rasRoleCache;

    public UserRoleService(SecurityContextService securityContextService,
                           AuthTokenGenerator authTokenGenerator,
                           CaseAssignmentApi caseAssignmentApi,
                           RoleAssignmentApi roleAssignmentApi) {
        this.securityContextService = securityContextService;
        this.authTokenGenerator = authTokenGenerator;
        this.caseAssignmentApi = caseAssignmentApi;
        this.roleAssignmentApi = roleAssignmentApi;
        this.rasRoleCache = Caffeine.newBuilder()
            .expireAfterWrite(RAS_ROLE_CACHE_TTL)
            .build();
    }

    public UserRoles getCurrentUserCaseRoles(long caseReference) {
        UserInfo currentUserDetails = securityContextService.getCurrentUserDetails();
        String currentUserId = currentUserDetails.getUid();

        Set<String> roles = new LinkedHashSet<>(safeRoles(currentUserDetails.getRoles()));
        if (!securityContextService.isSystemUser()) {
            roles.addAll(rasRoleCache.get(
                new CacheKey(caseReference, currentUserId),
                this::getRasRoles
            ));
        }

        return new UserRoles(UUID.fromString(currentUserId), List.copyOf(roles));
    }

    private Set<String> getRasRoles(CacheKey cacheKey) {
        String authorisation = securityContextService.getCurrentUserAuthToken();
        String serviceAuthorisation = authTokenGenerator.generate();
        String userId = cacheKey.userId();
        CaseAssignmentUserRolesResource caseAssignedUserRoles = caseAssignmentApi.getUserRoles(
            authorisation,
            serviceAuthorisation,
            List.of(String.valueOf(cacheKey.caseReference())),
            List.of(userId)
        );

        RoleAssignmentResponse roleAssignmentResponse = roleAssignmentApi.getRoles(
            serviceAuthorisation,
            authorisation,
            userId
        );

        Set<String> roles;

        if (caseAssignedUserRoles == null || caseAssignedUserRoles.getCaseAssignmentUserRoles() == null) {
            roles = new LinkedHashSet<>();
        } else {
            roles = caseAssignedUserRoles.getCaseAssignmentUserRoles().stream()
                .map(CaseAssignmentUserRole::getCaseRole)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        if (roleAssignmentResponse != null && !CollectionUtils.isEmpty(roleAssignmentResponse.getRoleAssignment())) {
            Set<String> roleAssignments = roleAssignmentResponse.getRoleAssignment().stream()
                .filter(UserRoleService::isNotSpecificGrantType)
                .map(RoleAssignment::getRoleName)
                .collect(Collectors.toCollection(LinkedHashSet::new));

            log.info("roleAssignments: {}", roleAssignments);

            roles.addAll(roleAssignments);
        }

        log.info("getRasRoles: {}", roles);
        return roles;
    }

    private static Collection<String> safeRoles(Collection<String> roles) {
        return roles != null ? roles : List.of();
    }

    private static boolean isNotSpecificGrantType(RoleAssignment roleAssignment) {
        return !Objects.equals(roleAssignment.getGrantType(), "SPECIFIC");
    }

    private record CacheKey(long caseReference, String userId) {
    }
}
