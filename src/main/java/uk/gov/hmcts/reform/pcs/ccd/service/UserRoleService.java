package uk.gov.hmcts.reform.pcs.ccd.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CaseAssignmentApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRole;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRolesResource;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerRoles;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class UserRoleService {

    private static final Duration RAS_ROLE_CACHE_TTL = Duration.ofMinutes(1);

    private static final String CITIZEN_ROLE = UserRole.CITIZEN.getRole();
    private static final String PCS_CASEWORKER_ROLE = UserRole.PCS_CASE_WORKER.getRole();
    private static final String PROFESSIONAL_ROLE_PREFIX = "pui-";

    /**
     * Staff and judicial roles. Currently never matched: these are RAS ORGANISATION assignments and
     * the roles seen here are IDAM userinfo plus CCD case roles only. Retained because it costs
     * nothing and becomes live if role sourcing gains an organisation-scope read.
     */
    private static final Set<String> INTERNAL_ROLES = Stream.concat(
        Arrays.stream(CaseworkerRoles.CASEWORKER_ROLES),
        Arrays.stream(JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES)
    ).map(UserRole::getRole).collect(Collectors.toUnmodifiableSet());

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
        if (!securityContextService.isSystemUser()) {
            roles.addAll(rasRoleCache.get(
                new CacheKey(caseReference, currentUserId),
                this::getRasRoles
            ));
        }

        return new UserRoles(UUID.fromString(currentUserId), List.copyOf(roles));
    }

    /**
     * Which of the three populations the caller belongs to. Decided only from signals that actually
     * reach this service - the IDAM roles on the bearer token, plus the caller's rd-professional
     * organisation, which the caller supplies.
     *
     * <p>Deliberately not keyed on a group-access role. {@code claimant-solicitor} and
     * {@code defendant-solicitor} are RAS ORGANISATION assignments; the roles here come from IDAM
     * userinfo and CCD {@code /case-users}, and the latter is filtered to CASE-type assignments. A
     * group role can never appear, so testing for its absence always passes and silently opens
     * whatever it guards.
     *
     * <p>{@link UserCapacity#INTERNAL} requires a positive internal signal rather than being the
     * fallback, so an account matching nothing is treated as external and sees less, not more.
     *
     * @param organisationId the caller's rd-professional organisation, null when they have none
     */
    public UserCapacity getCurrentUserCapacity(String organisationId) {
        Collection<String> roles = safeRoles(securityContextService.getCurrentUserDetails().getRoles());

        if (roles.contains(CITIZEN_ROLE)) {
            return UserCapacity.CITIZEN;
        }
        if (isExternalProfessional(roles, organisationId)) {
            return UserCapacity.PROFESSIONAL;
        }
        if (roles.stream().anyMatch(INTERNAL_ROLES::contains) || roles.contains(PCS_CASEWORKER_ROLE)) {
            return UserCapacity.INTERNAL;
        }
        return UserCapacity.CITIZEN;
    }

    private static boolean isExternalProfessional(Collection<String> roles, String organisationId) {
        return organisationId != null
            || roles.stream().anyMatch(role -> role.startsWith(PROFESSIONAL_ROLE_PREFIX));
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
