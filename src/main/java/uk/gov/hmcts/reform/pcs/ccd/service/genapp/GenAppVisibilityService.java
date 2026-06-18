package uk.gov.hmcts.reform.pcs.ccd.service.genapp;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.LegalRepresentativeRepository;

import java.util.Comparator;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@AllArgsConstructor
public class GenAppVisibilityService {

    private final LegalRepresentativeRepository legalRepresentativeRepository;

    public boolean isGenAppVisibleToUser(GenAppEntity genAppEntity, UUID currentUserId) {
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
        if (applicantParty == null || currentUserId == null) {
            return false;
        }

        if (currentUserId.equals(applicantParty.getIdamId())) {
            return true;
        }

        return legalRepresentativeRepository
            .isLegalRepresentativeLinkedToPartyAndActive(currentUserId, applicantParty.getId());
    }

    public List<GenAppEntity> getVisibleGenAppsToUser(Collection<GenAppEntity> genApps, UUID userId) {
        if (genApps == null || genApps.isEmpty()) {
            return List.of();
        }

        return genApps.stream()
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(
                GenAppEntity::getApplicationSubmittedDate,
                Comparator.nullsLast(Comparator.reverseOrder())
            ))
            .filter(genAppEntity -> isGenAppVisibleToUser(genAppEntity, userId))
            .toList();
    }
}
