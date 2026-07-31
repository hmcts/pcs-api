package uk.gov.hmcts.reform.pcs.ccd.service.genapp;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.LegalRepresentativeRepository;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@AllArgsConstructor
public class GenAppVisibilityService {

    private final LegalRepresentativeRepository legalRepresentativeRepository;

    private static final Set<String> INTERNAL_ROLES = Set.of(
        UserRole.JUDGE.getRole(),
        UserRole.FEE_PAID_JUDGE.getRole(),
        UserRole.CIRCUIT_JUDGE.getRole(),
        UserRole.LEADERSHIP_JUDGE.getRole(),
        UserRole.HEARING_CENTRE_TEAM_LEADER.getRole(),
        UserRole.HEARING_CENTRE_ADMIN.getRole(),
        UserRole.CTSC_TEAM_LEADER.getRole(),
        UserRole.CTSC_ADMIN.getRole(),
        UserRole.WLU_TEAM_LEADER.getRole(),
        UserRole.WLU_ADMIN.getRole()
    );

    public boolean isGenAppVisibleToUser(GenAppEntity genAppEntity, UUID currentUserId) {
        return isGenAppVisibleToUser(genAppEntity, currentUserId, List.of());
    }

    public boolean isGenAppVisibleToUser(GenAppEntity genAppEntity,
                                         UUID currentUserId,
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

        return isWithoutNoticeVisibleToUser(genAppEntity.getParty(), currentUserId, currentUserRoles);
    }

    public boolean isWithoutNoticeVisibleToUser(PartyEntity party,
                                                UUID currentUserId,
                                                Collection<String> currentUserRoles) {
        if (isInternalUser(currentUserRoles)) {
            return true;
        }

        if (party == null || currentUserId == null) {
            return false;
        }

        if (currentUserId.equals(party.getIdamId())) {
            return true;
        }

        return legalRepresentativeRepository
            .isLegalRepresentativeLinkedToPartyAndActive(currentUserId, party.getId());
    }

    public List<GenAppEntity> getVisibleGenAppsToUser(Collection<GenAppEntity> genApps, UUID userId) {
        return getVisibleGenAppsToUser(genApps, userId, List.of());
    }

    public List<GenAppEntity> getVisibleGenAppsToUser(Collection<GenAppEntity> genApps,
                                                      UUID userId,
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
            .filter(genAppEntity -> isGenAppVisibleToUser(genAppEntity, userId, currentUserRoles))
            .toList();
    }

    private boolean isInternalUser(Collection<String> currentUserRoles) {
        if (currentUserRoles == null || currentUserRoles.isEmpty()) {
            return false;
        }

        return currentUserRoles.stream().anyMatch(INTERNAL_ROLES::contains);
    }
}
