package uk.gov.hmcts.reform.pcs.enforcement.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;

import java.util.Arrays;
import java.util.Optional;

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
                .orElseThrow(() -> new IllegalArgumentException("Unknown enforcement type: " + code));
    }

    /**
     * The type recorded on an enforcement order, or null when it records no known type. Applications stored
     * before {@link EnforcementOrder#getChooseEnforcementType()} replaced it only carry the redundant
     * {@link EnforcementOrder#getSelectEnforcementType()}, so fall back to that.
     */
    public static SelectEnforcementType from(EnforcementOrder enforcementOrder) {
        if (enforcementOrder == null) {
            return null;
        }
        String code = Optional.ofNullable(enforcementOrder.getChooseEnforcementType())
            .map(DynamicStringList::getValueCode)
            .orElse(null);
        if (code == null) {
            return enforcementOrder.getSelectEnforcementType();
        }
        return Arrays.stream(values())
            .filter(type -> type.name().equals(code))
            .findFirst()
            .orElse(null);
    }
}
