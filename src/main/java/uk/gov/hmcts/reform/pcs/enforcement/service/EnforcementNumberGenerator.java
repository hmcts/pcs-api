package uk.gov.hmcts.reform.pcs.enforcement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;

/**
 * Gives an approved warrant or writ its number.
 *
 * <p>TODO HDPI-7352 / HDPI-7353: the warrant and writ number formats are still "TBC". Until they are agreed this
 * builds a unique placeholder: a type prefix and the next value of the enf_case_number_seq sequence, zero padded,
 * e.g. {@code WP-00000042}. Replace {@link #next} once the formats are known; nothing else depends on the format.
 */
@Component
@RequiredArgsConstructor
public class EnforcementNumberGenerator {

    static final String FORMAT = "%s-%08d";

    private final EnforcementOrderRepository enforcementOrderRepository;

    public String next(SelectEnforcementType enforcementType) {
        long sequenceValue = enforcementOrderRepository.nextEnforcementNumberSequenceValue();
        return FORMAT.formatted(prefix(enforcementType), sequenceValue);
    }

    static String prefix(SelectEnforcementType enforcementType) {
        return switch (enforcementType) {
            case WARRANT -> "WP";
            case WARRANT_OF_RESTITUTION -> "WR";
            case WRIT -> "HP";
            case WRIT_OF_RESTITUTION -> "HR";
        };
    }
}
