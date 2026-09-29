package uk.gov.hmcts.reform.pcs.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.notify.template.EmailTemplate;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NotificationTemplateConfigurationTest {
    private static final String LEGACY_TEMPLATE_ID = "8ecf44d2-860f-44e3-a891-c82745f79645";
    private final EmailTemplate emailTemplate = EmailTemplate.RESPONSE_NO_COUNTERCLAIM;

    private NotificationTemplateConfiguration config;
    private FeatureToggleService featureToggleService;

    @BeforeEach
    void setUp() {
        featureToggleService = mock(FeatureToggleService.class);
        config = new NotificationTemplateConfiguration(featureToggleService);
    }

    @Test
    void shouldUseLegacyTemplateWhenReleaseFlagDisabled() {
        config.setTemplates(Map.of("response-no-counterclaim", "new-template"));
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4)).thenReturn(false);

        String result = config.getTemplateId(emailTemplate);

        assertEquals(LEGACY_TEMPLATE_ID, result);
    }

    @Test
    void shouldThrowExceptionWhenReleaseEnabledAndTemplateKeyMissing() {
        config.setTemplates(Map.of("something-else", "template-123"));
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> config.getTemplateId(emailTemplate)
        );

        assertTrue(ex.getMessage().contains("Missing release 1.4 template"));
    }

    @Test
    void shouldThrowExceptionWhenReleaseEnabledAndTemplatesNotConfigured() {
        config.setTemplates(null);
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4)).thenReturn(true);

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> config.getTemplateId(emailTemplate)
        );

        assertTrue(ex.getMessage().contains("not configured"));
    }

    @Test
    void shouldUseReleaseTemplateWhenReleaseFlagEnabled() {
        config.setTemplates(Map.of("response-no-counterclaim", "new-template"));
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4)).thenReturn(true);

        assertEquals("new-template", config.getTemplateId(emailTemplate));
    }

    @Test
    void shouldUseLegacyTemplateWhenReleaseFlagEnabledWithoutEnvironmentOverride() {
        config.setTemplates(Map.of("response-no-counterclaim", LEGACY_TEMPLATE_ID));
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4)).thenReturn(true);

        assertEquals(LEGACY_TEMPLATE_ID, config.getTemplateId(emailTemplate));
    }

    @Test
    void shouldUseLegacyTemplateWithoutEnvironmentOverride() {
        config.setTemplates(null);

        assertEquals(LEGACY_TEMPLATE_ID, config.getTemplateId(emailTemplate));
    }

    @Test
    void shouldLoadLegacyDefaultsForAllTemplates() {
        for (EmailTemplate template : EmailTemplate.values()) {
            assertDoesNotThrow(() -> UUID.fromString(config.getTemplateId(template)));
        }
    }

    @Test
    void shouldRejectMissingReleaseTemplatesWhenReleaseFlagEnabled() {
        config.setTemplates(Map.of("response-no-counterclaim", ""));
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4)).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> config.getTemplateId(emailTemplate)
        );

        assertTrue(exception.getMessage().contains("Missing release 1.4 template"));
    }
}
