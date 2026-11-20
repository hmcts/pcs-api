package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.DefaultResourceLoader;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HlpasLeafletProviderTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "  ")
    @DisplayName("Has no leaflet while no location is configured")
    void shouldHaveNoLeafletWhileUnconfigured(String location) {
        HlpasLeafletProvider provider = new HlpasLeafletProvider(new DefaultResourceLoader(), location);

        assertThat(provider.getLeaflet()).isEmpty();
    }

    @Test
    @DisplayName("Loads the leaflet from its configured location")
    void shouldLoadTheConfiguredLeaflet() {
        HlpasLeafletProvider provider = new HlpasLeafletProvider(new DefaultResourceLoader(),
            "classpath:forms/ONC-003-counterclaim.pdf");

        assertThat(provider.getLeaflet()).hasValueSatisfying(bytes ->
            assertThat(bytes).startsWith("%PDF".getBytes()));
    }

    @Test
    @DisplayName("Loads the leaflet from a file location")
    void shouldLoadTheLeafletFromAFile() throws Exception {
        Path leaflet = Files.createTempFile("hlpas", ".pdf");
        Files.write(leaflet, "leaflet".getBytes());
        try {
            HlpasLeafletProvider provider = new HlpasLeafletProvider(new DefaultResourceLoader(),
                leaflet.toUri().toString());

            assertThat(provider.getLeaflet())
                .hasValueSatisfying(bytes -> assertThat(bytes).isEqualTo("leaflet".getBytes()));
        } finally {
            Files.delete(leaflet);
        }
    }

    @Test
    @DisplayName("Fails at startup when the configured leaflet cannot be read")
    void shouldFailWhenTheConfiguredLeafletIsMissing() {
        DefaultResourceLoader resourceLoader = new DefaultResourceLoader();

        assertThatThrownBy(() -> new HlpasLeafletProvider(resourceLoader, "classpath:forms/no-such-leaflet.pdf"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("HLPAS legal advice leaflet");
    }
}
