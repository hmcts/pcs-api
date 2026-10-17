package uk.gov.hmcts.reform.pcs.enforcement.notification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.yaml.snakeyaml.Yaml;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class EnforcementEmailTest {

    @ParameterizedTest
    @EnumSource(EnforcementEmail.class)
    void shouldNameTheTemplateAndTheConfigurationKey(EnforcementEmail email) {
        assertThat(email.getTemplateName()).isNotBlank();
        assertThat(email.getTemplateKey()).isNotBlank().matches("[a-z][a-z-]*[a-z]");
    }

    @Test
    void shouldHaveADistinctConfigurationKeyForEveryEmail() {
        Set<String> keys = Arrays.stream(EnforcementEmail.values())
            .map(EnforcementEmail::getTemplateKey)
            .collect(Collectors.toSet());

        assertThat(keys).hasSameSizeAs(EnforcementEmail.values());
    }

    /**
     * Every email needs a key in application.yaml, or its template id could never be configured and it would
     * silently never send.
     */
    @Test
    void shouldHaveAConfigurationKeyForEveryEmailInApplicationYaml() {
        assertThat(configuredTemplateKeys())
            .containsExactlyInAnyOrderElementsOf(Arrays.stream(EnforcementEmail.values())
                                                     .map(EnforcementEmail::getTemplateKey)
                                                     .toList());
    }

    /**
     * No template id is set in any environment: none of these templates has been created in GOV.UK Notify,
     * which has no API for creating one, so nothing can be emailed yet.
     */
    @Test
    void shouldHaveNoTemplateIdConfiguredByDefault() {
        assertThat(configuredTemplates().values())
            .allSatisfy(templateId -> assertThat(templateId).matches("\\$\\{[A-Z0-9_]+:}"));
    }

    @Test
    void shouldDeclareThePersonalisationEachTemplateNeeds() {
        assertThat(EnforcementEmail.WARRANT_APPROVED.getPersonalisation())
            .containsExactly(EnforcementEmail.ENFORCEMENT_NUMBER);
        assertThat(EnforcementEmail.WRIT_APPROVED.getPersonalisation())
            .containsExactly(EnforcementEmail.ENFORCEMENT_NUMBER);
        assertThat(EnforcementEmail.APPLICATION_REJECTED.getPersonalisation())
            .containsExactly(EnforcementEmail.REJECTION_REASON);
        assertThat(EnforcementEmail.EVICTION_SCHEDULED.getPersonalisation())
            .containsExactly(EnforcementEmail.EVICTION_DATE_TIME);
        assertThat(EnforcementEmail.EVICTION_RESCHEDULED.getPersonalisation())
            .containsExactly(EnforcementEmail.EVICTION_DATE_TIME);
        assertThat(EnforcementEmail.CLAIMANT_CONFIRMED_ATTENDANCE.getPersonalisation())
            .containsExactly(EnforcementEmail.EVICTION_DATE_TIME);
        assertThat(EnforcementEmail.WARRANT_RETURN.getPersonalisation())
            .containsExactly(EnforcementEmail.RETURN_NAME, EnforcementEmail.WARRANT_RETURN_BODY);
    }

    @Test
    void shouldHaveAnApprovalEmailForAWarrantAndAWritOnly() {
        assertThat(EnforcementEmail.forApproval(SelectEnforcementType.WARRANT))
            .isEqualTo(EnforcementEmail.WARRANT_APPROVED);
        assertThat(EnforcementEmail.forApproval(SelectEnforcementType.WRIT))
            .isEqualTo(EnforcementEmail.WRIT_APPROVED);
        assertThat(EnforcementEmail.forApproval(SelectEnforcementType.WARRANT_OF_RESTITUTION)).isNull();
        assertThat(EnforcementEmail.forApproval(SelectEnforcementType.WRIT_OF_RESTITUTION)).isNull();
        assertThat(EnforcementEmail.forApproval(null)).isNull();
    }

    @Test
    void shouldHaveARejectionEmailForAWarrantOnly() {
        assertThat(EnforcementEmail.forRejection(SelectEnforcementType.WARRANT))
            .isEqualTo(EnforcementEmail.APPLICATION_REJECTED);
        assertThat(EnforcementEmail.forRejection(SelectEnforcementType.WRIT)).isNull();
        assertThat(EnforcementEmail.forRejection(SelectEnforcementType.WARRANT_OF_RESTITUTION)).isNull();
        assertThat(EnforcementEmail.forRejection(SelectEnforcementType.WRIT_OF_RESTITUTION)).isNull();
        assertThat(EnforcementEmail.forRejection(null)).isNull();
    }

    private static List<String> configuredTemplateKeys() {
        return List.copyOf(configuredTemplates().keySet());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> configuredTemplates() {
        try (InputStream applicationYaml = EnforcementEmailTest.class.getClassLoader()
            .getResourceAsStream("application.yaml")) {
            Map<String, Object> yaml = new Yaml().load(applicationYaml);
            Map<String, Object> enforcementNotify = (Map<String, Object>) yaml.get("enforcement-notify");
            return (Map<String, String>) enforcementNotify.get("templates");
        } catch (Exception ex) {
            throw new IllegalStateException("Could not read enforcement-notify.templates from application.yaml", ex);
        }
    }
}
