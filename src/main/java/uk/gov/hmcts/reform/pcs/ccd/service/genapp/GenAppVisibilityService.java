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
    private static final String PCS_SOLICITOR_ROLE = UserRole.PCS_SOLICITOR.getRole();

    public boolean isGenAppVisibleToUser(GenAppEntity genAppEntity,
                                         String organisationId) {
        return isGenAppVisibleToUser(genAppEntity, organisationId, List.of());
    }

    public boolean isGenAppVisibleToUser(GenAppEntity genAppEntity,
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

        return isWithoutNoticeVisibleToUser(genAppEntity.getParty(), organisationId, currentUserRoles);
    }

    public boolean isWithoutNoticeVisibleToUser(PartyEntity party,
                                                String organisationId,
                                                Collection<String> currentUserRoles) {
        if (isInternalUser(currentUserRoles)) {
            return true;
        }

        if (party == null || organisationId == null) {
            return false;
        }

        if (organisationId.equals(party.getOrganisationId())) {
            return true;
        }

        return organisationRepository
            .isOrganisationLinkedToPartyAndActive(organisationId, party.getId());
    }

    public boolean isGenAppDocumentVisibleToUser(GenAppEntity genAppEntity,
                                                 String organisationId,
                                                 Collection<String> currentUserRoles) {
        if (genAppEntity == null) {
            return false;
        }

        if (genAppEntity.getWithoutNotice() == VerticalYesNo.YES) {
            return isWithoutNoticeVisibleToUser(genAppEntity.getParty(), organisationId, currentUserRoles);
        }

        return isGenAppVisibleToUser(genAppEntity, organisationId, currentUserRoles);
    }

    public List<GenAppEntity> getVisibleGenAppsToUser(Collection<GenAppEntity> genApps, String organisationId) {
        return getVisibleGenAppsToUser(genApps, organisationId, List.of());
    }

    public List<GenAppEntity> getVisibleGenAppsToUser(Collection<GenAppEntity> genApps,
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
            .filter(genAppEntity -> isGenAppVisibleToUser(genAppEntity, organisationId, currentUserRoles))
            .toList();
    }

    private boolean isInternalUser(Collection<String> currentUserRoles) {
        if (currentUserRoles == null || currentUserRoles.isEmpty()) {
            return false;
        }

        return currentUserRoles.stream().anyMatch(INTERNAL_ROLES::contains)
            || isPcsCaseworkerWithoutSolicitorRole(currentUserRoles);
    }

    private boolean isPcsCaseworkerWithoutSolicitorRole(Collection<String> currentUserRoles) {
        return currentUserRoles.contains(PCS_CASEWORKER_ROLE)
            && !currentUserRoles.contains(PCS_SOLICITOR_ROLE);
    }
}
