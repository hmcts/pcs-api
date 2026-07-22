package uk.gov.hmcts.reform.pcs.ccd.service.genapp;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.LegalRepresentativeOrganisationRepository;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@AllArgsConstructor
public class GenAppVisibilityService {

    private final LegalRepresentativeOrganisationRepository legalRepresentativeOrganisationRepository;

    public boolean isGenAppVisibleToUser(GenAppEntity genAppEntity, String organisationId) {
        if (genAppEntity == null) {
            return false;
        }

        if (genAppEntity.getState() != GenAppState.GEN_APP_ISSUED) {
            return false;
        }

        if (genAppEntity.getWithoutNotice() != VerticalYesNo.YES) {
            return true;
        }

        PartyEntity applicantParty = genAppEntity.getParty();
        if (applicantParty == null || organisationId == null) {
            return false;
        }

        if (organisationId.equals(applicantParty.getOrganisationId())) {
            return true;
        }

        return legalRepresentativeOrganisationRepository
            .isRepresentativeOrganisationLinkedToPartyAndActive(organisationId, applicantParty.getId());
    }

    public List<GenAppEntity> getVisibleGenAppsToUser(Collection<GenAppEntity> genApps, String organisationId) {
        if (genApps == null || genApps.isEmpty()) {
            return List.of();
        }

        return genApps.stream()
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(
                GenAppEntity::getApplicationSubmittedDate,
                Comparator.nullsLast(Comparator.reverseOrder())
            ))
            .filter(genAppEntity -> isGenAppVisibleToUser(genAppEntity, organisationId))
            .toList();
    }
}
