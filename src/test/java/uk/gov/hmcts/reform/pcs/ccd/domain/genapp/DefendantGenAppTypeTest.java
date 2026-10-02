package uk.gov.hmcts.reform.pcs.ccd.domain.genapp;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class DefendantGenAppTypeTest {

    @ParameterizedTest
    @EnumSource(DefendantGenAppType.class)
    void shouldConvertToGenAppType(DefendantGenAppType defendantGenAppType) {
        assertThat(defendantGenAppType.toGenAppType().name()).isEqualTo(defendantGenAppType.name());
    }
}
