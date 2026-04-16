package uk.gov.hmcts.reform.pcs.ccd.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VerticalYesNoTest {

    @Test
    @SuppressWarnings("ConstantValue")
    void shouldReturnEnumForBoolean() {
        assertThat(SimpleYesNo.from(true)).isEqualTo(SimpleYesNo.YES);
        assertThat(SimpleYesNo.from(false)).isEqualTo(SimpleYesNo.NO);
        assertThat(SimpleYesNo.from(null)).isEqualTo(null);
    }

    @Test
    void shouldConvertToBoolean() {
        assertThat(SimpleYesNo.YES.toBoolean()).isTrue();
        assertThat(SimpleYesNo.NO.toBoolean()).isFalse();
    }

}
