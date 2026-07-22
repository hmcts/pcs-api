package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;

/**
 * Resolves exempt-landlord answers while {@code landlord_registered} is renamed to {@code exempt_landlord}.
 * Compat phase: accept {@code exemptLandlord} from CCD/FE but persist to {@code landlord_registered}.
 */
public final class ExemptLandlordResolver {

    private ExemptLandlordResolver() {
    }

    public static YesNoNotSure fromResponses(DefendantResponses responses) {
        if (responses == null) {
            return null;
        }
        if (responses.getExemptLandlord() != null) {
            return responses.getExemptLandlord();
        }
        return responses.getLandlordRegistered();
    }

    public static YesNoNotSure fromEntity(DefendantResponseEntity entity) {
        if (entity == null) {
            return null;
        }
        return entity.getLandlordRegistered();
    }
}
