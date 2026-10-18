package uk.gov.hmcts.reform.pcs.ccd.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CaseAssignmentApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRole;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRolesResource;
import uk.gov.hmcts.reform.pcs.am.RoleAssignment;
import uk.gov.hmcts.reform.pcs.am.RoleAssignmentApi;
import uk.gov.hmcts.reform.pcs.am.RoleAssignmentAttributes;
import uk.gov.hmcts.reform.pcs.am.RoleAssignmentResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.OrganisationUserRoles;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserRoleService {

    private static final Duration RAS_ROLE_CACHE_TTL = Duration.ofMinutes(1);

    private final SecurityContextService securityContextService;
    private final AuthTokenGenerator authTokenGenerator;
    private final CaseAssignmentApi caseAssignmentApi;
    private final RoleAssignmentApi roleAssignmentApi;
    private final Cache<CacheKey, Set<String>> caseRoleCache;
    private final Cache<String, List<OrganisationalRole>> organisationalRoleCache;

    public UserRoleService(SecurityContextService securityContextService,
                           AuthTokenGenerator authTokenGenerator,
                           CaseAssignmentApi caseAssignmentApi,
                           RoleAssignmentApi roleAssignmentApi) {
        this.securityContextService = securityContextService;
        this.authTokenGenerator = authTokenGenerator;
        this.caseAssignmentApi = caseAssignmentApi;
        this.roleAssignmentApi = roleAssignmentApi;
        this.caseRoleCache = Caffeine.newBuilder()
            .expireAfterWrite(RAS_ROLE_CACHE_TTL)
            .build();
        this.organisationalRoleCache = Caffeine.newBuilder()
            .expireAfterWrite(RAS_ROLE_CACHE_TTL)
            .build();
    }

    /**
     * IDAM roles, case roles on this case and AM organisational roles, without any group-access role: with
     * no case access groups given, nothing says which cases a firm-wide role applies to. Use
     * {@link #getCurrentUserCaseRoles(long, Collection)} when the caller needs to know which side of the case
     * the user is on.
     */
    public UserRoles getCurrentUserCaseRoles(long caseReference) {
        return getCurrentUserCaseRoles(caseReference, Set.of());
    }

    /**
     * IDAM roles, case roles on this case and AM organisational roles. A group-access role
     * ({@link OrganisationUserRoles#ORGANISATION_ROLES}, or any assignment carrying a {@code caseAccessGroupId})
     * is held by every user in the organisation for every case the organisation is on, on either side, so it
     * counts only when its {@code caseAccessGroupId} is one of this case's {@code caseAccessGroupIds}.
     */
    public UserRoles getCurrentUserCaseRoles(long caseReference, Collection<String> caseAccessGroupIds) {
        UserInfo currentUserDetails = securityContextService.getCurrentUserDetails();
        String currentUserId = currentUserDetails.getUid();
        Collection<String> caseGroups = caseAccessGroupIds == null ? Set.of() : caseAccessGroupIds;

        Set<String> roles = new LinkedHashSet<>(safeRoles(currentUserDetails.getRoles()));
        if (!securityContextService.isSystemUser()) {
            roles.addAll(caseRoleCache.get(
                new CacheKey(caseReference, currentUserId),
                key -> getCaseRoles(key.caseReference(), key.userId())
            ));
            getOrganisationalRoles(currentUserId).stream()
                .filter(role -> role.appliesToCase(caseGroups))
                .map(OrganisationalRole::roleName)
                .forEach(roles::add);
        }

        return new UserRoles(securityContextService.toUserId(currentUserId), List.copyOf(roles));
    }

    /**
     * IDAM roles plus the current user's AM organisational role assignments, with no case in scope.
     * Callers that need to know what kind of user this is - rather than what they can do on one case
     * - can use this without paying for the per-case lookup. Group-access roles are included whatever case
     * they apply to.
     */
    public UserRoles getCurrentUserOrganisationalRoles() {
        UserInfo currentUserDetails = securityContextService.getCurrentUserDetails();
        String currentUserId = currentUserDetails.getUid();

        Set<String> roles = new LinkedHashSet<>(safeRoles(currentUserDetails.getRoles()));
        if (!securityContextService.isSystemUser()) {
            getOrganisationalRoles(currentUserId).stream()
                .map(OrganisationalRole::roleName)
                .forEach(roles::add);
        }

        return new UserRoles(securityContextService.toUserId(currentUserId), List.copyOf(roles));
    }

    private List<OrganisationalRole> getOrganisationalRoles(String userId) {
        return organisationalRoleCache.get(userId, this::getRoleAssignments);
    }

    private Set<String> getCaseRoles(Long caseId, String userId) {
        CaseAssignmentUserRolesResource caseAssignedUserRoles = caseAssignmentApi.getUserRoles(
            securityContextService.getCurrentUserAuthToken(),
            authTokenGenerator.generate(),
            List.of(String.valueOf(caseId)),
            List.of(userId)
        );

        if (caseAssignedUserRoles == null || caseAssignedUserRoles.getCaseAssignmentUserRoles() == null) {
            return Set.of();
        }

        return caseAssignedUserRoles.getCaseAssignmentUserRoles().stream()
            .map(CaseAssignmentUserRole::getCaseRole)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<OrganisationalRole> getRoleAssignments(String userId) {
        RoleAssignmentResponse roleAssignmentResponse = roleAssignmentApi.getRoles(
            authTokenGenerator.generate(),
            securityContextService.getCurrentUserAuthToken(),
            userId
        );

        if (roleAssignmentResponse == null || CollectionUtils.isEmpty(roleAssignmentResponse.getRoleAssignment())) {
            return List.of();
        }

        return roleAssignmentResponse.getRoleAssignment().stream()
            .filter(UserRoleService::isNotSpecificGrantType)
            .map(OrganisationalRole::of)
            .distinct()
            .toList();
    }

    private static Collection<String> safeRoles(Collection<String> roles) {
        return roles != null ? roles : List.of();
    }

    private static boolean isNotSpecificGrantType(RoleAssignment roleAssignment) {
        return !Objects.equals(roleAssignment.getGrantType(), "SPECIFIC");
    }

    private record CacheKey(long caseReference, String userId) {
    }

    /**
     * An AM organisational role name and, for a group-access role, the case access group it applies to.
     */
    private record OrganisationalRole(String roleName, String caseAccessGroupId) {

        static OrganisationalRole of(RoleAssignment roleAssignment) {
            RoleAssignmentAttributes attributes = roleAssignment.getAttributes();
            String caseAccessGroupId = attributes == null ? null : attributes.getCaseAccessGroupId();
            return new OrganisationalRole(roleAssignment.getRoleName(),
                                          StringUtils.isBlank(caseAccessGroupId) ? null : caseAccessGroupId);
        }

        boolean appliesToCase(Collection<String> caseAccessGroupIds) {
            if (caseAccessGroupId == null) {
                return !OrganisationUserRoles.ORGANISATION_ROLES.contains(roleName);
            }
            return caseAccessGroupIds.contains(caseAccessGroupId);
        }
    }
}
