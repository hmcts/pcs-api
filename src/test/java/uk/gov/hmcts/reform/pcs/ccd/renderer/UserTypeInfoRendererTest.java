package uk.gov.hmcts.reform.pcs.ccd.renderer;

import io.pebbletemplates.pebble.PebbleEngine;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import io.pebbletemplates.pebble.loader.ClasspathLoader;

class UserTypeInfoRendererTest {

    @Test
    void shouldRenderTemplateWithPebbleEngine() throws Exception {
        // Given
        ClasspathLoader loader = new ClasspathLoader();
        loader.setPrefix("templates/");
        loader.setSuffix(".peb");
        PebbleEngine engine = new PebbleEngine.Builder()
            .loader(loader)
            .build();
        UserTypeInfoRenderer renderer = new UserTypeInfoRenderer(engine);
        String userType = "CLAIMANT";
        long caseReference = 1234L;

        // When
        String result = renderer.render(userType, caseReference);

        // Then
        assertThat(result).contains("User Type:");
        assertThat(result).contains("CLAIMANT");
        assertThat(result).contains("Case Reference:");
        assertThat(result).contains("1234");
    }
}
