package uk.gov.hmcts.reform.pcs.ccd.util;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import static org.assertj.core.api.Assertions.assertThat;

class YesOrNoBooleanTest {

    @Test
    void shouldReturnTrueWhenYesOrNoIsYes() {
        Boolean result = YesOrNoToBoolean.convert(YesOrNo.YES);
        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseWhenYesOrNoIsNo() {
        Boolean result = YesOrNoToBoolean.convert(YesOrNo.NO);
        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnNullWhenYesOrNoIsNull() {
        Boolean result = YesOrNoToBoolean.convert((YesOrNo) null);
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnTrueWhenVerticalYesNoIsYes() {
        Boolean result = YesOrNoToBoolean.convert(VerticalYesNo.YES);
        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseWhenVerticalYesNoIsNo() {
        Boolean result = YesOrNoToBoolean.convert(VerticalYesNo.NO);
        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnNullWhenVerticalYesNoIsNull() {
        Boolean result = YesOrNoToBoolean.convert((VerticalYesNo) null);
        assertThat(result).isNull();
    }
}
