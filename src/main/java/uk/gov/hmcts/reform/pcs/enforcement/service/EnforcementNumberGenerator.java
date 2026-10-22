package uk.gov.hmcts.reform.pcs.enforcement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;

@Component
@RequiredArgsConstructor
public class EnforcementNumberGenerator {

    // TODO: HDPI-7352 / HDPI-7353 will replace this placeholder warrant and writ number format
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
