package uk.gov.hmcts.reform.pcs.ccd3.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VerticalYesNoTest {

    @Test
    @SuppressWarnings("ConstantValue")
    void shouldReturnEnumForBoolean() {
        assertThat(VerticalYesNo.from(true)).isEqualTo(VerticalYesNo.YES);
        assertThat(VerticalYesNo.from(false)).isEqualTo(VerticalYesNo.NO);
        assertThat(VerticalYesNo.from(null)).isEqualTo(null);
    }

    @Test
    void shouldConvertToBoolean() {
        assertThat(VerticalYesNo.YES.toBoolean()).isTrue();
        assertThat(VerticalYesNo.NO.toBoolean()).isFalse();
    }

}
