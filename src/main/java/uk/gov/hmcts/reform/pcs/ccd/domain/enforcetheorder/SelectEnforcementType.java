package uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

import java.util.Arrays;

@AllArgsConstructor
@Getter
public enum SelectEnforcementType implements HasLabel {

    WARRANT("Warrant of possession"),
    WRIT("Writ of possession"),
    WARRANT_OF_RESTITUTION("Warrant of restitution"),
    WRIT_OF_RESTITUTION("Writ of restitution");

    private final String label;

    public static SelectEnforcementType getSelectEnforcementTypeFromName(String code) {
        return Arrays.stream(values())
                .filter(type -> type.name().equals(code))
                .findFirst()
                .orElse(null);
    }
}
