package uk.gov.hmcts.reform.pcs.ccd.service.genapp;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.OrganisationRepository;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerRoles.CASEWORKER_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;

@Service
@AllArgsConstructor
public class GenAppVisibilityService {

    private final OrganisationRepository organisationRepository;

    private static final Set<String> INTERNAL_ROLES = Stream.concat(
        Arrays.stream(CASEWORKER_ROLES),
        Arrays.stream(JUDICIAL_HISTORY_ROLES)
    ).map(UserRole::getRole).collect(Collectors.toUnmodifiableSet());
    private static final String PCS_CASEWORKER_ROLE = UserRole.PCS_CASE_WORKER.getRole();
    private static final String PROFESSIONAL_ROLE_PREFIX = "pui-";

    public boolean isGenAppVisibleToUser(GenAppEntity genAppEntity,
                                         UUID userId,
                                         String organisationId,
                                         Collection<String> currentUserRoles) {
        if (genAppEntity == null) {
            return false;
        }

        if (genAppEntity.getState() != GenAppState.GEN_APP_ISSUED) {
            return false;
        }

        if (genAppEntity.getWithoutNotice() != VerticalYesNo.YES) {
            return true;
        }

        return isWithoutNoticeVisibleToUser(genAppEntity.getParty(), userId, organisationId, currentUserRoles);
    }

    public boolean isWithoutNoticeVisibleToUser(PartyEntity party,
                                                UUID userId,
                                                String organisationId,
                                                Collection<String> currentUserRoles) {

        if (isInternalUser(currentUserRoles, organisationId)) {
            return true;
        }

        if (party == null || userId == null) {
            return false;
        }

        if (userId.equals(party.getIdamId())) {
            return true;
        }

        if (organisationId == null) {
            return false;
        }

        if (organisationId.equals(party.getOrganisationId())) {
            return true;
        }

        return organisationRepository
            .isOrganisationLinkedToPartyAndActive(organisationId, party.getId());
    }

    public boolean isGenAppDocumentVisibleToUser(GenAppEntity genAppEntity,
                                                 UUID userId,
                                                 String organisationId,
                                                 Collection<String> currentUserRoles) {
        if (genAppEntity == null) {
            return false;
        }

        if (genAppEntity.getWithoutNotice() == VerticalYesNo.YES) {
            return isWithoutNoticeVisibleToUser(genAppEntity.getParty(), userId, organisationId, currentUserRoles);
        }

        return isGenAppVisibleToUser(genAppEntity, userId, organisationId, currentUserRoles);
    }

    public List<GenAppEntity> getVisibleGenAppsToUser(Collection<GenAppEntity> genApps,
                                                      UUID userId,
                                                      String organisationId) {
        return getVisibleGenAppsToUser(genApps, userId, organisationId, List.of());
    }

    public List<GenAppEntity> getVisibleGenAppsToUser(Collection<GenAppEntity> genApps,
                                                      UUID userId,
                                                      String organisationId,
                                                      Collection<String> currentUserRoles) {
        if (genApps == null || genApps.isEmpty()) {
            return List.of();
        }

        return genApps.stream()
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(
                GenAppEntity::getApplicationSubmittedDate,
                Comparator.nullsLast(Comparator.reverseOrder())
            ))
            .filter(genAppEntity -> isGenAppVisibleToUser(genAppEntity, userId, organisationId, currentUserRoles))
            .toList();
    }

    private boolean isInternalUser(Collection<String> currentUserRoles, String organisationId) {
        if (currentUserRoles == null || currentUserRoles.isEmpty()) {
            return false;
        }

        return currentUserRoles.stream().anyMatch(INTERNAL_ROLES::contains)
            || isPcsCaseworkerWithoutOrganisation(currentUserRoles, organisationId);
    }

    /**
     * An HMCTS caseworker, as opposed to an external professional who also holds
     * {@code caseworker-pcs}. Both signals must agree before the caller is treated as internal, so
     * any hint of being external falls through to the party and organisation checks below.
     *
     * <p>Deliberately not keyed on a group-access role: those are RAS ORGANISATION assignments and
     * never reach {@code currentUserRoles}, which is IDAM userinfo plus CCD case roles only. Testing
     * for their absence would always pass and silently open this guard.
     */
    private boolean isPcsCaseworkerWithoutOrganisation(Collection<String> currentUserRoles,
                                                       String organisationId) {
        return currentUserRoles.contains(PCS_CASEWORKER_ROLE)
            && organisationId == null
            && currentUserRoles.stream().noneMatch(role -> role.startsWith(PROFESSIONAL_ROLE_PREFIX));
    }
}
