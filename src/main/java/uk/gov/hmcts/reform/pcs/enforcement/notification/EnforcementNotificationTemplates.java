package uk.gov.hmcts.reform.pcs.enforcement.notification;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * The Notify template ids for the enforcement emails, keyed by {@link EnforcementEmail#getTemplateKey()}
 * under {@code enforcement-notify.templates}.
 *
 * <p>Unlike {@link uk.gov.hmcts.reform.pcs.config.NotificationTemplateConfiguration}, a missing id is not an
 * error. None of the enforcement templates has been written in Notify yet, so every key is blank in every
 * environment, and an unconfigured email has to be skipped rather than fail the case event that would have
 * sent it.</p>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "enforcement-notify")
public class EnforcementNotificationTemplates {

    private Map<String, String> templates = new LinkedHashMap<>();

    /**
     * The template id configured for the email, or empty when the key is absent or blank.
     */
    public Optional<String> templateId(EnforcementEmail email) {
        if (email == null || templates == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(templates.get(email.getTemplateKey())).filter(id -> isNotBlank(id));
    }
}
