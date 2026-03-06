package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.strategy;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType;

import java.util.Map;

@Component
@AllArgsConstructor
public class EnforcementTypeStrategyFactory {

    private final WarrantStrategy warrantStrategy;
    private final WritStrategy writStrategy;
    private final WritOfRestitutionStrategy writOfRestitutionStrategy;
    private final WarrantOfRestitutionStrategy warrantOfRestitutionStrategy;

    public EnforcementTypeStrategy getStrategy(SelectEnforcementType type) {
        return Map.of(
            SelectEnforcementType.WARRANT, warrantStrategy,
            SelectEnforcementType.WRIT, writStrategy,
            SelectEnforcementType.WRIT_OF_RESTITUTION, writOfRestitutionStrategy,
            SelectEnforcementType.WARRANT_OF_RESTITUTION, warrantOfRestitutionStrategy
        ).get(type);
    }

}
