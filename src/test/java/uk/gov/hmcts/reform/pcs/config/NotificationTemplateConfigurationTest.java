package uk.gov.hmcts.reform.pcs.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.notify.template.EmailTemplate;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NotificationTemplateConfigurationTest {
    private static final String LEGACY_TEMPLATE_ID = "8ecf44d2-860f-44e3-a891-c82745f79645";
    private static final String RELEASE_TEMPLATE_ID = "new-template-123";
    private static final EmailTemplate TEST_EMAIL_TEMPLATE = EmailTemplate.RESPONSE_NO_COUNTERCLAIM;

    private NotificationTemplateConfiguration config;
    private FeatureToggleService featureToggleService;

    @BeforeEach
    void setUp() {
        featureToggleService = mock(FeatureToggleService.class);
        config = new NotificationTemplateConfiguration(featureToggleService);
        config.setLegacyTemplates(Map.of("response-no-counterclaim", LEGACY_TEMPLATE_ID));
    }

    @Test
    void shouldUseLegacyTemplateWhenReleaseFlagDisabled() {
        config.setTemplates(Map.of("response-no-counterclaim", RELEASE_TEMPLATE_ID));
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4)).thenReturn(false);

        String result = config.getTemplateId(TEST_EMAIL_TEMPLATE);

        assertEquals(LEGACY_TEMPLATE_ID, result);
    }

    @Test
    void shouldThrowExceptionWhenLegacyTemplatesNotConfiguredAndReleaseFlagDisabled() {
        config.setLegacyTemplates(null);
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4)).thenReturn(false);

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> config.getTemplateId(TEST_EMAIL_TEMPLATE)
        );

        assertTrue(ex.getMessage().contains("Legacy notification templates are not configured"));
    }

    @Test
    void shouldThrowExceptionWhenLegacyTemplateKeyMissingAndReleaseFlagDisabled() {
        config.setLegacyTemplates(Map.of("something-else", "template-123"));
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> config.getTemplateId(TEST_EMAIL_TEMPLATE)
        );

        assertTrue(ex.getMessage().contains("Missing legacy template"));
    }

    @Test
    void shouldUseReleaseTemplateWhenReleaseFlagEnabled() {
        config.setTemplates(Map.of("response-no-counterclaim", RELEASE_TEMPLATE_ID));
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4)).thenReturn(true);

        String result = config.getTemplateId(TEST_EMAIL_TEMPLATE);

        assertEquals(RELEASE_TEMPLATE_ID, result);
    }

    @Test
    void shouldThrowExceptionWhenReleaseTemplatesNotConfiguredAndReleaseFlagEnabled() {
        config.setTemplates(null);
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4)).thenReturn(true);

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> config.getTemplateId(TEST_EMAIL_TEMPLATE)
        );

        assertTrue(exception.getMessage().contains("Notification templates are not configured"));
    }

    @Test
    void shouldThrowExceptionWhenReleaseTemplateKeyMissingAndReleaseFlagEnabled() {
        config.setTemplates(Map.of("something-else", "template-123"));
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4)).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> config.getTemplateId(TEST_EMAIL_TEMPLATE)
        );

        assertTrue(exception.getMessage().contains("Missing release 1.4 template"));
    }

    @Test
    void shouldRejectEmptyStringTemplateValueAndReleaseFlagEnabled() {
        config.setTemplates(Map.of("response-no-counterclaim", ""));
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4)).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> config.getTemplateId(TEST_EMAIL_TEMPLATE)
        );

        assertTrue(exception.getMessage().contains("Missing release 1.4 template"));
    }

    @Test
    void shouldRejectEmptyStringLegacyTemplateValueAndReleaseFlagDisabled() {
        config.setLegacyTemplates(Map.of("response-no-counterclaim", ""));
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> config.getTemplateId(TEST_EMAIL_TEMPLATE)
        );

        assertTrue(exception.getMessage().contains("Missing legacy template"));
    }
}
