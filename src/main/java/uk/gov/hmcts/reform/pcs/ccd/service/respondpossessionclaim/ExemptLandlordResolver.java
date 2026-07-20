package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;

/**
 * Resolves exempt-landlord answers while {@code landlord_registered} is renamed to {@code exempt_landlord}.
 * Cleanup phase: read and persist {@code exempt_landlord} only.
 */
public final class ExemptLandlordResolver {

    private ExemptLandlordResolver() {
    }

    public static YesNoNotSure fromResponses(DefendantResponses responses) {
        if (responses == null) {
            return null;
        }
        return responses.getExemptLandlord();
    }

    public static YesNoNotSure fromEntity(DefendantResponseEntity entity) {
        if (entity == null) {
            return null;
        }
        return entity.getExemptLandlord();
    }
}
