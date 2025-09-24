package uk.gov.hmcts.reform.pcs.config;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.extension.core.DisallowExtensionCustomizerBuilder;
import io.pebbletemplates.pebble.loader.ClasspathLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class PebbleConfiguration {

    // To mitigate CVE-2025-1686 until the Pebble library is updated with a fix
    @Bean
    public PebbleEngine pebbleEngine() {
        ClasspathLoader loader = new ClasspathLoader();
        loader.setPrefix("templates/");
        loader.setSuffix(".peb");
        return new PebbleEngine.Builder()
            .loader(loader)
            .registerExtensionCustomizer(new DisallowExtensionCustomizerBuilder()
                                             .disallowedTokenParserTags(List.of("include"))
                                             .build())
            .build();
    }

}
