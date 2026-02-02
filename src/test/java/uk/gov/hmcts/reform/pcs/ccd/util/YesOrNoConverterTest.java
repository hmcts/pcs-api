package uk.gov.hmcts.reform.pcs.ccd.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class YesOrNoConverterTest {

    @ParameterizedTest
    @MethodSource("yesOrNoToBooleanScenarios")
    void shouldConvertYesOrNoToBoolean(YesOrNo yesOrNo, Boolean expectedResult) {
        Boolean actualResult = YesOrNoConverter.toBoolean(yesOrNo);

        assertThat(actualResult).isEqualTo(expectedResult);
    }

    @ParameterizedTest
    @MethodSource("verticalYesOrNoToBooleanScenarios")
    void shouldConvertVerticalYesOrNoToBoolean(VerticalYesNo yesOrNo, Boolean expectedResult) {
        Boolean actualResult = YesOrNoConverter.toBoolean(yesOrNo);

        assertThat(actualResult).isEqualTo(expectedResult);
    }

    @ParameterizedTest
    @MethodSource("yesOrNoToVerticalYesNoScenarios")
    void shouldConvertYesOrNoToVerticalYesNo(YesOrNo yesOrNo, VerticalYesNo expectedResult) {
        VerticalYesNo actualResult = YesOrNoConverter.toVerticalYesNo(yesOrNo);

        assertThat(actualResult).isEqualTo(expectedResult);
    }

    private static Stream<Arguments> yesOrNoToBooleanScenarios() {
        return Stream.of(
            arguments(YesOrNo.YES, true),
            arguments(YesOrNo.NO, false),
            arguments(null, null)
        );
    }

    private static Stream<Arguments> verticalYesOrNoToBooleanScenarios() {
        return Stream.of(
            arguments(VerticalYesNo.YES, true),
            arguments(VerticalYesNo.NO, false),
            arguments(null, null)
        );
    }

    private static Stream<Arguments> yesOrNoToVerticalYesNoScenarios() {
        return Stream.of(
            arguments(YesOrNo.YES, VerticalYesNo.YES),
            arguments(YesOrNo.NO, VerticalYesNo.NO),
            arguments(null, null)
        );
    }

}
