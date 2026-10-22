package uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SelectEnforcementTypeTest {

    @ParameterizedTest
    @EnumSource(SelectEnforcementType.class)
    void shouldReturnEnumForValidName(SelectEnforcementType type) {
        SelectEnforcementType result =
            SelectEnforcementType.getSelectEnforcementTypeFromName(type.name());

        assertThat(result).isSameAs(type);
    }

    @Test
    void shouldThrowExceptionForUnknownName() {
        String code = "Unknown";
        assertThatThrownBy(() -> SelectEnforcementType.getSelectEnforcementTypeFromName(code))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unknown enforcement type: " + code);
    }

    @Test
    void shouldThrowExceptionForNullName() {
        assertThatThrownBy(() -> SelectEnforcementType.getSelectEnforcementTypeFromName(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unknown enforcement type: null");
    }
}
