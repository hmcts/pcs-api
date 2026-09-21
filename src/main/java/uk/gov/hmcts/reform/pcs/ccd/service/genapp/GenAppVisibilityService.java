package uk.gov.hmcts.reform.pcs.ccd.service.genapp;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.OrganisationRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.UserCapacity;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoleService;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


@Service
@AllArgsConstructor
public class GenAppVisibilityService {

    private final OrganisationRepository organisationRepository;
    private final UserRoleService userRoleService;

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

        return userRoleService.getCurrentUserCapacity(organisationId) == UserCapacity.INTERNAL;
    }
}
