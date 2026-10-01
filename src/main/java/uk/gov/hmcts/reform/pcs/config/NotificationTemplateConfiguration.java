package uk.gov.hmcts.reform.pcs.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.notify.template.EmailTemplate;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "notify")
@RequiredArgsConstructor
public class NotificationTemplateConfiguration {

    private final FeatureToggleService featureToggleService;

    private Map<String, String> templates;
    private Map<String, String> legacyTemplates;

    public boolean isRelease14Enabled() {
        return featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4);
    }

    public String getTemplateId(EmailTemplate template) {
        if (isRelease14Enabled()) {
            return getRelease14TemplateId(template);
        }

        return getLegacyTemplateId(template);
    }

    private String getRelease14TemplateId(EmailTemplate template) {
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

    private String getLegacyTemplateId(EmailTemplate template) {
        if (legacyTemplates == null || legacyTemplates.isEmpty()) {
            throw new IllegalStateException("Legacy notification templates are not configured");
        }

        String templateId = legacyTemplates.get(template.getTemplateKey());
        if (templateId == null || templateId.isBlank()) {
            throw new IllegalArgumentException(
                "Missing legacy template for key: " + template.getTemplateKey()
            );
        }

        return templateId;
    }
}
