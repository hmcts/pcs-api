package uk.gov.hmcts.reform.pcs.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.notify.template.EmailTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NotificationTemplateConfigurationTest {
    private final EmailTemplate emailTemplate = EmailTemplate.RESPONSE_NO_COUNTERCLAIM;

    private NotificationTemplateConfiguration config;

    @BeforeEach
    void setUp() {
        config = new NotificationTemplateConfiguration();
    }

    @Test
    void shouldReturnTemplateIdWhenTemplateExists() {
        config.setTemplates(Map.of("response-no-counterclaim", "template-123"));

        String result = config.getTemplateId(emailTemplate);

        assertEquals("template-123", result);
    }

    @Test
    void shouldThrowExceptionWhenTemplateKeyMissing() {
        config.setTemplates(Map.of("something-else", "template-123"));

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> config.getTemplateId(emailTemplate)
        );

        assertTrue(ex.getMessage().contains("Missing template"));
    }

    @Test
    void shouldThrowExceptionWhenTemplatesNotConfigured() {
        config.setTemplates(null);

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> config.getTemplateId(emailTemplate)
        );

        assertTrue(ex.getMessage().contains("not configured"));
    }
}
