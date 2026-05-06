package uk.gov.hmcts.reform.pcs.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.notify.template.EmailTemplate;

import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "notify")
public class NotificationTemplateConfiguration {
    private Map<String, String> templates;

    public String getTemplateId(EmailTemplate template) {
        if (templates == null || templates.isEmpty()) {
            throw new IllegalStateException("Notification templates are not configured");
        }

        String templateId = templates.get(template.getTemplateKey());

        if (templateId == null) {
            throw new IllegalArgumentException(
                "Missing template for key: " + template.getTemplateKey()
            );
        }

        return templateId;
    }
}
