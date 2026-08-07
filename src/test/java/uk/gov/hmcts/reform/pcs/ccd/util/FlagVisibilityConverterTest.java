package uk.gov.hmcts.reform.pcs.ccd.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.ccd.sdk.type.FlagVisibility;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.util.FlagVisibilityConverter.toFlagVisibility;

class FlagVisibilityConverterTest {

    @ParameterizedTest
    @ValueSource(strings = {"External", "external", "EXTERNAL"})
    void shouldResolveExternalVisibilityIgnoringCase(String visibility) {
        assertThat(toFlagVisibility(visibility)).isEqualTo(FlagVisibility.EXTERNAL);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Internal", "internal", "INTERNAL"})
    void shouldResolveInternalVisibilityIgnoringCase(String visibility) {
        assertThat(toFlagVisibility(visibility)).isEqualTo(FlagVisibility.INTERNAL);
    }

    @ParameterizedTest
    @NullSource
    void shouldDefaultToInternalVisibilityWhenNotSet(String visibility) {
        assertThat(toFlagVisibility(visibility)).isEqualTo(FlagVisibility.INTERNAL);
    }

    @Test
    void shouldDefaultToInternalVisibilityForUnrecognisedValue() {
        assertThat(toFlagVisibility("Restricted")).isEqualTo(FlagVisibility.INTERNAL);
    }

    @Test
    void shouldDefaultToInternalVisibilityForBlankValue() {
        assertThat(toFlagVisibility("")).isEqualTo(FlagVisibility.INTERNAL);
    }
}
