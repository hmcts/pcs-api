package uk.gov.hmcts.reform.pcs.enforcement.service;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.pcs.am.RoleAssignment;
import uk.gov.hmcts.reform.pcs.am.RoleAssignmentApi;
import uk.gov.hmcts.reform.pcs.am.RoleAssignmentAttributes;
import uk.gov.hmcts.reform.pcs.am.RoleAssignmentResponse;
import uk.gov.hmcts.reform.pcs.ccd.CaseType;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Finds the bailiff chosen in the "Find the bailiff's name" type-ahead. XUI's StaffUser type-ahead searches
 * staff reference data by role category, which cannot tell a bailiff from other admin staff, so the choice
 * is checked here against the user's AM organisational roles. The access control configuration qualifies the
 * bailiff role with {@code @Jurisdiction PCS}, so a bailiff in another jurisdiction is not one of ours. The
 * name and email come from IDAM.
 *
 * <p>TODO(HDPI-7356): limit the bailiffs to the case's court location once that rule is agreed
 * (OPEN-QUESTIONS 24), by checking the role assignment's primaryLocation or region.
 */
@Slf4j
@Service
public class BailiffDirectoryService {

    static final String ORGANISATION_ROLE_TYPE = "ORGANISATION";

    private final RoleAssignmentApi roleAssignmentApi;
    private final IdamClient idamClient;
    private final AuthTokenGenerator authTokenGenerator;
    private final IdamTokenProvider systemUpdateUserTokenProvider;

    public BailiffDirectoryService(RoleAssignmentApi roleAssignmentApi,
                                   IdamClient idamClient,
                                   AuthTokenGenerator authTokenGenerator,
                                   @Qualifier("systemUpdateUserTokenProvider")
                                   IdamTokenProvider systemUpdateUserTokenProvider) {
        this.roleAssignmentApi = roleAssignmentApi;
        this.idamClient = idamClient;
        this.authTokenGenerator = authTokenGenerator;
        this.systemUpdateUserTokenProvider = systemUpdateUserTokenProvider;
    }

    /**
     * The user with this IDAM id, if they have the bailiff organisational role.
     */
    public Optional<BailiffUser> findBailiff(String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }
        String systemUserToken = systemUpdateUserTokenProvider.getAuthToken();
        if (!hasBailiffRole(userId, systemUserToken)) {
            log.info("User {} was chosen as a bailiff but has no bailiff role", userId);
            return Optional.empty();
        }
        try {
            UserDetails user = idamClient.getUserByUserId(systemUserToken, userId);
            return Optional.of(new BailiffUser(userId, fullName(user), user.getEmail()));
        } catch (FeignException.NotFound ex) {
            log.info("Bailiff {} has a role assignment but no IDAM account", userId);
            return Optional.empty();
        }
    }

    /**
     * Whether the user with this IDAM id has the bailiff organisational role, looked up in AM now.
     */
    public boolean isBailiff(String userId) {
        if (userId == null || userId.isBlank()) {
            return false;
        }
        return hasBailiffRole(userId, systemUpdateUserTokenProvider.getAuthToken());
    }

    private boolean hasBailiffRole(String userId, String systemUserToken) {
        RoleAssignmentResponse response =
            roleAssignmentApi.getRoles(authTokenGenerator.generate(), systemUserToken, userId);
        List<RoleAssignment> assignments = response == null ? null : response.getRoleAssignment();
        return assignments != null && assignments.stream().anyMatch(BailiffDirectoryService::isBailiffRole);
    }

    private static boolean isBailiffRole(RoleAssignment assignment) {
        RoleAssignmentAttributes attributes = assignment.getAttributes();
        return ORGANISATION_ROLE_TYPE.equals(assignment.getRoleType())
            && UserRole.BAILIFF.getRole().equals(assignment.getRoleName())
            && attributes != null
            && CaseType.getJurisdictionId().equals(attributes.getJurisdiction());
    }

    private static String fullName(UserDetails user) {
        return Stream.of(user.getForename(), user.getSurname().orElse(null))
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(part -> !part.isEmpty())
            .collect(Collectors.joining(" "));
    }
}
