package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates that immutable Party fields are not sent by frontend during draft saves.
 * These fields are claimant-entered metadata that must not be modified by defendants.
 *
 * <p>Security model:
 * - Validation catches non-null violations (returns errors to frontend)
 * - NON_NULL serialization excludes null fields from draft JSON
 * - Draft merge preserves original database values for excluded fields
 */
@Service
@Slf4j
public class ImmutablePartyFieldValidator {

    /**
     * Finds immutable Party fields that were incorrectly sent with non-null values.
     *
     * <p>Immutable fields (claimant-entered, defendant must not modify):
     * - nameKnown
     * - addressKnown
     * - addressSameAsProperty
     *
     * @param party Party data from frontend payload
     * @param caseReference Case reference for logging
     * @return List of field names with non-null values (empty if valid)
     */
    public List<String> findImmutableFieldViolations(Party party, long caseReference) {
        List<String> violations = new ArrayList<>();

        if (party == null) {
            return violations;
        }

        if (party.getNameKnown() != null) {
            violations.add("nameKnown");
            log.error("Immutable field violation for case {}: nameKnown should not be sent", caseReference);
        }

        if (party.getAddressKnown() != null) {
            violations.add("addressKnown");
            log.error("Immutable field violation for case {}: addressKnown should not be sent", caseReference);
        }

        if (party.getAddressSameAsProperty() != null) {
            violations.add("addressSameAsProperty");
            log.error("Immutable field violation for case {}: addressSameAsProperty should not be sent", caseReference);
        }

        return violations;
    }
}
