package uk.gov.hmcts.reform.pcs.enforcement.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SelectEnforcementTypeTest {

    @ParameterizedTest
    @EnumSource(SelectEnforcementType.class)
    void shouldFindEveryTypeByName(SelectEnforcementType enforcementType) {
        assertThat(SelectEnforcementType.getSelectEnforcementTypeFromName(enforcementType.name()))
            .isEqualTo(enforcementType);
    }

    @Test
    void shouldRejectAnUnknownName() {
        assertThatThrownBy(() -> SelectEnforcementType.getSelectEnforcementTypeFromName("WARRANT_OF_SOMETHING"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unknown enforcement type: WARRANT_OF_SOMETHING");
    }
}
