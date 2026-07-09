package uk.gov.hmcts.reform.pcs.config;

import com.launchdarkly.sdk.LDContext;
import com.launchdarkly.sdk.server.LDClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LaunchDarklyConfigurationTest {

    private final LaunchDarklyConfiguration underTest = new LaunchDarklyConfiguration();

    @Test
    void shouldRunOfflineWhenOfflineModeTrueEvenWithKeyPresent() throws Exception {
        // LAUNCHDARKLY_OFFLINE=true forces offline regardless of a configured key (used by local/cftlibTest)
        try (LDClient client = underTest.ldClient("sdk-key-present", true, new String[0])) {
            assertThat(client.isInitialized()).isTrue();

            LDContext context = LDContext.create("test");
            assertThat(client.boolVariation("any-flag", context, true)).isTrue();
            assertThat(client.boolVariation("any-flag", context, false)).isFalse();
        }
    }

    @Test
    void shouldServeFlagValuesFromFileDataSource(@TempDir Path dir) throws Exception {
        Path flagFile = Files.writeString(dir.resolve("flags.json"),
            "{ \"flagValues\": { \"bulk-print-enabled\": true } }");

        // a file data source loads flags from disk instead of the network, so the key is unused
        try (LDClient client = underTest.ldClient("dummy-key", true, new String[]{flagFile.toString()})) {
            assertThat(client.boolVariation("bulk-print-enabled", LDContext.create("test"), false)).isTrue();
        }
    }

    @Test
    void shouldIgnoreBlankAndMissingFlagFilesButUseTheValidOne(@TempDir Path dir) throws Exception {
        Path flagFile = Files.writeString(dir.resolve("flags.json"),
            "{ \"flagValues\": { \"bulk-print-enabled\": true } }");

        // blank entries and non-existent paths are filtered out; only the real file drives the data source
        String[] files = {"", dir.resolve("does-not-exist.json").toString(), flagFile.toString()};
        try (LDClient client = underTest.ldClient("dummy-key", true, files)) {
            assertThat(client.boolVariation("bulk-print-enabled", LDContext.create("test"), false)).isTrue();
        }
    }

    @Test
    void shouldBuildOfflineClientWithoutDataSourceWhenNoFlagFilesProvided() throws Exception {
        // key present + not offline + no flag files => no file data source is attached; without a
        // connection the client stays uninitialised and every evaluation falls back to the passed default
        try (LDClient client = underTest.ldClient("dummy-key", true, new String[0])) {
            assertThat(client.isInitialized()).isTrue();
            assertThat(client.boolVariation("any-flag", LDContext.create("test"), true)).isTrue();
        }
    }
}
