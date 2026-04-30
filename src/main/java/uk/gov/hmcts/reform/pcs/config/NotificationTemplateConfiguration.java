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
        return templates.get(template.getTemplateKey());
    }
}
