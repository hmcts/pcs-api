package uk.gov.hmcts.reform.pcs.enforcement.accesscontrol;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.type.CaseAccessGroup;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.InternalTabAccess;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoleService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.gov.hmcts.reform.pcs.ccd.util.CaseAccessGroupsUtil.deriveCaseAccessGroups;

/**
 * Decides which side of a case the current user views enforcement from.
 *
 * <p>CCD field access cannot do this on its own. A legal representative on either side holds
 * {@code caseworker-pcs-solicitor}, which the claimant-only enforcement fields are granted on, and group-access
 * roles are held firm-wide, so a firm acting for the defendant on this claim may act for the claimant on
 * another. So the side comes from the user's case roles on this case, counting a group-access role only when
 * it is for one of this case's access groups.
 *
 * <p>It fails closed: a user who holds neither an internal role nor a claimant-side role on this case - for
 * instance because their case roles could not be looked up - is {@link EnforcementViewer#RESTRICTED}, as is
 * anyone holding a defendant-side role.
 */
@Service
@RequiredArgsConstructor
public class EnforcementViewerService {

    private static final Set<String> DEFENDANT_SIDE_ROLES = roles(
        UserRole.DEFENDANT,
        UserRole.DEFENDANT_SOLICITOR,
        UserRole.GA_DEFENDANT_SOLICITOR
    );

    private static final Set<String> CLAIMANT_SIDE_ROLES = roles(
        UserRole.CREATOR,
        UserRole.CLAIMANT_SOLICITOR,
        UserRole.GA_CLAIMANT_SOLICITOR,
        UserRole.CLAIMANT
    );

    /**
     * The access profiles the internal enforcement fields are granted on.
     */
    private static final Set<String> INTERNAL_ACCESS_PROFILES = new InternalTabAccess().getGrants().keySet()
        .stream()
        .map(HasRole::getRole)
        .collect(Collectors.toUnmodifiableSet());

    /**
     * PCS's and Work Allocation's system users read the whole case: bulk and batch print fetch enforcement
     * documents through CDAM as PCS's system user, and CDAM only serves a document the user can see on the case.
     */
    private static final Set<String> SYSTEM_ROLES = roles(UserRole.SYSTEM_USER, UserRole.WA_SYSTEM_USER);

    private static final Map<String, UserRole> USER_ROLES = Arrays.stream(UserRole.values())
        .collect(Collectors.toUnmodifiableMap(UserRole::getRole, Function.identity()));

    private final UserRoleService userRoleService;
    private final SecurityContextService securityContextService;

    public EnforcementViewer currentViewer(PcsCaseEntity pcsCaseEntity, PCSCase pcsCase, long caseReference) {
        if (securityContextService.isSystemUser() || securityContextService.isIdamSystemUser()) {
            return EnforcementViewer.INTERNAL;
        }
        Collection<String> roles = userRoleService
            .getCurrentUserCaseRoles(caseReference, caseAccessGroupIds(pcsCaseEntity, pcsCase))
            .roles();
        return viewerWith(roles == null ? List.of() : roles);
    }

    static EnforcementViewer viewerWith(Collection<String> roles) {
        if (roles.stream().anyMatch(DEFENDANT_SIDE_ROLES::contains)) {
            return EnforcementViewer.RESTRICTED;
        }
        if (roles.stream().anyMatch(CLAIMANT_SIDE_ROLES::contains)) {
            return EnforcementViewer.CLAIMANT;
        }
        if (roles.stream().anyMatch(EnforcementViewerService::isInternal)) {
            return EnforcementViewer.INTERNAL;
        }
        return EnforcementViewer.RESTRICTED;
    }

    private static boolean isInternal(String role) {
        if (SYSTEM_ROLES.contains(role)) {
            return true;
        }
        UserRole userRole = USER_ROLES.get(role);
        return userRole != null
            && Arrays.stream(userRole.getAccessProfiles()).anyMatch(INTERNAL_ACCESS_PROFILES::contains);
    }

    /**
     * The access groups the case view gives CCD, derived the same way, since PCSCaseView sets them only after
     * the case view contributors have run.
     */
    private static Set<String> caseAccessGroupIds(PcsCaseEntity pcsCaseEntity, PCSCase pcsCase) {
        if (pcsCaseEntity == null || pcsCaseEntity.getParties() == null) {
            return Set.of();
        }
        List<ListValue<CaseAccessGroup>> groups =
            deriveCaseAccessGroups(pcsCaseEntity.getParties(), pcsCase == null ? null : pcsCase.getAllDefendants());
        return groups.stream()
            .map(ListValue::getValue)
            .filter(Objects::nonNull)
            .map(CaseAccessGroup::getCaseAccessGroupId)
            .filter(Objects::nonNull)
            .collect(Collectors.toUnmodifiableSet());
    }

    private static Set<String> roles(UserRole... roles) {
        return Stream.of(roles).map(UserRole::getRole).collect(Collectors.toUnmodifiableSet());
    }
}
