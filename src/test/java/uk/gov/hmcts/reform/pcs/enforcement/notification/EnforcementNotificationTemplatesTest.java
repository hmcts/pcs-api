package uk.gov.hmcts.reform.pcs.enforcement.notification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EnforcementNotificationTemplatesTest {

    @Test
    void shouldReturnTheConfiguredTemplateId() {
        EnforcementNotificationTemplates templates = templates(Map.of("warrant-approved", "a-template-id"));

        assertThat(templates.templateId(EnforcementEmail.WARRANT_APPROVED)).contains("a-template-id");
    }

    @ParameterizedTest
    @EnumSource(EnforcementEmail.class)
    void shouldHaveNoTemplateIdWhenNothingIsConfigured(EnforcementEmail email) {
        assertThat(templates(Map.of()).templateId(email)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    void shouldTreatABlankTemplateIdAsUnconfigured(String templateId) {
        EnforcementNotificationTemplates templates = templates(Map.of("warrant-approved", templateId));

        assertThat(templates.templateId(EnforcementEmail.WARRANT_APPROVED)).isEmpty();
    }

    @Test
    void shouldHaveNoTemplateIdForNoEmail() {
        assertThat(templates(Map.of("warrant-approved", "a-template-id")).templateId(null)).isEmpty();
    }

    @Test
    void shouldHaveNoTemplateIdWhenTheTemplateMapIsAbsent() {
        EnforcementNotificationTemplates templates = new EnforcementNotificationTemplates();
        templates.setTemplates(null);

        assertThat(templates.templateId(EnforcementEmail.WARRANT_APPROVED)).isEmpty();
    }

    private static EnforcementNotificationTemplates templates(Map<String, String> configured) {
        EnforcementNotificationTemplates templates = new EnforcementNotificationTemplates();
        templates.setTemplates(new HashMap<>(configured));
        return templates;
    }
}
