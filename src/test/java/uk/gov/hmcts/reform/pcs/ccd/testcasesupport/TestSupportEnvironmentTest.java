package uk.gov.hmcts.reform.pcs.ccd.testcasesupport;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TestSupportEnvironmentTest {

    @Test
    void shouldEnableForDevEnvironment() {
        boolean enabled = TestSupportEnvironment.isNonProdTestSupportEnabled("dev", null, null, null);

        assertThat(enabled).isTrue();
    }

    @Test
    void shouldEnableForPreviewEnvironment() {
        boolean enabled = TestSupportEnvironment.isNonProdTestSupportEnabled("preview", null, null, null);

        assertThat(enabled).isTrue();
    }

    @Test
    void shouldEnableForAatEnvironment() {
        boolean enabled = TestSupportEnvironment.isNonProdTestSupportEnabled("aat", null, null, null);

        assertThat(enabled).isTrue();
    }

    @Test
    void shouldEnableForDevSpringProfile() {
        boolean enabled = TestSupportEnvironment.isNonProdTestSupportEnabled(null, "dev", null, null);

        assertThat(enabled).isTrue();
    }

    @Test
    void shouldEnableWhenTestingSupportFlagIsTrue() {
        boolean enabled = TestSupportEnvironment.isNonProdTestSupportEnabled(null, null, "true", null);

        assertThat(enabled).isTrue();
    }

    @Test
    void shouldEnableWhenTestingEnforcementFlagIsTrue() {
        boolean enabled = TestSupportEnvironment.isNonProdTestSupportEnabled(null, null, null, "true");

        assertThat(enabled).isTrue();
    }

    @Test
    void shouldDisableWhenNoNonProdIndicatorsSet() {
        boolean enabled = TestSupportEnvironment.isNonProdTestSupportEnabled(null, "prod", "false", "false");

        assertThat(enabled).isFalse();
    }
}

