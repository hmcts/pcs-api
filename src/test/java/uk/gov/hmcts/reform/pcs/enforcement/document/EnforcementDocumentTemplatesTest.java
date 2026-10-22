package uk.gov.hmcts.reform.pcs.enforcement.document;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EnforcementDocumentTemplatesTest {

    @Test
    void shouldGiveTheConfiguredTemplate() {
        EnforcementDocumentTemplates templates = new EnforcementDocumentTemplates();
        templates.setTemplates(Map.of("warrant-of-possession", "CV-PCS-ENF-Warrant.docx"));

        assertThat(templates.templateId(EnforcementOutput.WARRANT_OF_POSSESSION)).contains("CV-PCS-ENF-Warrant.docx");
    }

    @Test
    void shouldGiveNoTemplateWhenTheKeyIsMissingOrBlank() {
        EnforcementDocumentTemplates templates = new EnforcementDocumentTemplates();
        Map<String, String> configured = new HashMap<>();
        configured.put("writ-of-possession", " ");
        templates.setTemplates(configured);

        assertThat(templates.templateId(EnforcementOutput.WRIT_OF_POSSESSION)).isEmpty();
        assertThat(templates.templateId(EnforcementOutput.WARRANT_OF_POSSESSION)).isEmpty();
        assertThat(templates.templateId(null)).isEmpty();
    }

    @Test
    void shouldConfigureATemplateForEveryOutputInApplicationYaml() throws IOException {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
            .load("application", new ClassPathResource("application.yaml"));
        StandardEnvironment environment = new StandardEnvironment();
        sources.forEach(environment.getPropertySources()::addLast);
        EnforcementDocumentTemplates templates = Binder.get(environment)
            .bind("enforcement.documents", EnforcementDocumentTemplates.class)
            .orElseThrow(IllegalStateException::new);

        for (EnforcementOutput output : EnforcementOutput.values()) {
            assertThat(templates.templateId(output)).as(output.getTemplateKey()).isPresent();
        }
    }
}
