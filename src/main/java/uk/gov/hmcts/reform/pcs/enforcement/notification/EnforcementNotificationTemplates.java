package uk.gov.hmcts.reform.pcs.enforcement.notification;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "enforcement-notify")
public class EnforcementNotificationTemplates {

    private Map<String, String> templates = new LinkedHashMap<>();

    public Optional<String> templateId(EnforcementEmail email) {
        if (email == null || templates == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(templates.get(email.getTemplateKey())).filter(id -> isNotBlank(id));
    }
}
