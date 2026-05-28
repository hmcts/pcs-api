package uk.gov.hmcts.reform.pcs.ccd.util;

import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;

import java.util.Comparator;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class GenAppUtils {

    private GenAppUtils() {
    }

    public static boolean isVisibleToUser(GenAppEntity genAppEntity, UUID userId) {
        if (genAppEntity == null) {
            return false;
        }

        if (genAppEntity.getWithoutNotice() != VerticalYesNo.YES) {
            return true;
        }

        return userId != null
            && genAppEntity.getParty() != null
            && userId.equals(genAppEntity.getParty().getIdamId());
    }

    public static List<GenAppEntity> getVisibleGenAppsForUser(
        Collection<GenAppEntity> genApps,
        UUID userId
    ) {
        if (genApps == null || genApps.isEmpty()) {
            return List.of();
        }

        return genApps.stream()
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(
                GenAppEntity::getApplicationSubmittedDate,
                Comparator.nullsLast(Comparator.reverseOrder())
            ))
            .filter(genAppEntity -> isVisibleToUser(genAppEntity, userId))
            .toList();
    }
}
