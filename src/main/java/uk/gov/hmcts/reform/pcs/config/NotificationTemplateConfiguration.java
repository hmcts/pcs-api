package uk.gov.hmcts.reform.pcs.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.notify.template.EmailTemplate;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "notify")
@RequiredArgsConstructor
public class NotificationTemplateConfiguration {
    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\$\\{[^:}]+:([^]]+)}");
    private static final Map<String, String> LEGACY_TEMPLATES = loadLegacyTemplates();

    private final FeatureToggleService featureToggleService;
    private Map<String, String> templates;

    public boolean isRelease14Enabled() {
        return featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4);
    }

    public String getTemplateId(EmailTemplate template) {
        if (!featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4)) {
            return LEGACY_TEMPLATES.get(template.getTemplateKey());
        }

        if (templates == null || templates.isEmpty()) {
            throw new IllegalStateException("Notification templates are not configured");
        }

        String templateId = templates.get(template.getTemplateKey());

        if (templateId == null || templateId.isBlank()) {
            throw new IllegalArgumentException(
                "Missing release 1.4 template for key: " + template.getTemplateKey()
            );
        }

        return templateId;
    }

    private static Map<String, String> loadLegacyTemplates() {
        List<PropertySource<?>> sources;
        try {
            sources = new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yaml"));
        } catch (IOException e) {
            throw new IllegalStateException("Could not load notification template defaults", e);
        }

        Map<String, String> templates = new HashMap<>();
        for (EmailTemplate template : EmailTemplate.values()) {
            String propertyName = "notify.templates." + template.getTemplateKey();
            String configuredValue = sources.stream()
                .map(source -> source.getProperty(propertyName))
                .filter(Objects::nonNull)
                .map(Object::toString)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing template configuration: " + propertyName));

            Matcher matcher = TEMPLATE_PATTERN.matcher(configuredValue);
            if (!matcher.matches()) {
                throw new IllegalStateException("Missing default template ID in application.yaml: " + propertyName);
            }
            templates.put(template.getTemplateKey(), matcher.group(1));
        }
        return Map.copyOf(templates);
    }
}
