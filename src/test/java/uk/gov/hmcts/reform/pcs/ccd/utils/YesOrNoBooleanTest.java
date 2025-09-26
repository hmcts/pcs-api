package uk.gov.hmcts.reform.pcs.ccd.utils;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import uk.gov.hmcts.ccd.sdk.type.YesOrNo;

class YesOrNoBooleanTest {

    @Test
    void shouldReturnTrueForYes() {
        // When
        Boolean result = YesOrNoToBoolean.convert(YesOrNo.YES);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseForNo() {
        // When
        Boolean result = YesOrNoToBoolean.convert(YesOrNo.NO);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnNullForNull() {
        // When
        Boolean result = YesOrNoToBoolean.convert(null);

        // Then
        assertThat(result).isNull();
    }
}
