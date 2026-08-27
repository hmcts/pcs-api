package uk.gov.hmcts.reform.pcs.ccd.domain.genapp;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ClaimantGenAppTypeTest {

    @ParameterizedTest
    @MethodSource("typeConversionScenarios")
    void shouldConvertToGenAppType(ClaimantGenAppType claimantGenAppType,
                                   GenAppType expectedGenAppType) {

        assertThat(claimantGenAppType.toGenAppType()).isEqualTo(expectedGenAppType);
    }

    private static Stream<Arguments> typeConversionScenarios() {
        return Stream.of(
            Arguments.arguments(ClaimantGenAppType.ADJOURN, GenAppType.ADJOURN),
            Arguments.arguments(ClaimantGenAppType.SET_ASIDE, GenAppType.SET_ASIDE),
            Arguments.arguments(ClaimantGenAppType.SOMETHING_ELSE, GenAppType.SOMETHING_ELSE)
        );
    }

}
