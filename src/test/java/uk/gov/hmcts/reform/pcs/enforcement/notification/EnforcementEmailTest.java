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
import java.util.UUID;
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
        assertThat(EnforcementEmail.WARRANT_OF_RESTITUTION_APPROVED.getPersonalisation()).isEmpty();
        assertThat(EnforcementEmail.WARRANT_REJECTED.getPersonalisation())
            .containsExactly(EnforcementEmail.REJECTION_REASON);
        assertThat(EnforcementEmail.WRIT_REJECTED.getPersonalisation()).isEmpty();
        assertThat(EnforcementEmail.WARRANT_OF_RESTITUTION_REJECTED.getPersonalisation()).isEmpty();
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
    void shouldHaveAnApplicationReceivedEmailForEveryType() {
        assertThat(EnforcementEmail.forApplicationReceived(SelectEnforcementType.WARRANT))
            .isEqualTo(EnforcementEmail.WARRANT_APPLICATION_RECEIVED);
        assertThat(EnforcementEmail.forApplicationReceived(SelectEnforcementType.WRIT))
            .isEqualTo(EnforcementEmail.WRIT_APPLICATION_RECEIVED);
        assertThat(EnforcementEmail.forApplicationReceived(SelectEnforcementType.WARRANT_OF_RESTITUTION))
            .isEqualTo(EnforcementEmail.WARRANT_OF_RESTITUTION_APPLICATION_RECEIVED);
        assertThat(EnforcementEmail.forApplicationReceived(SelectEnforcementType.WRIT_OF_RESTITUTION))
            .isEqualTo(EnforcementEmail.WRIT_OF_RESTITUTION_APPLICATION_RECEIVED);
        assertThat(EnforcementEmail.forApplicationReceived(null)).isNull();
    }

    /**
     * The dev profile, which bootWithCCD and cftlibTest run with, sends every email to the local Notify stub.
     */
    @Test
    void shouldHaveAStandInTemplateIdForEveryEmailInTheDevProfile() {
        Map<String, String> devTemplates = templatesIn("application-dev.yaml");

        assertThat(devTemplates.keySet()).containsExactlyInAnyOrderElementsOf(configuredTemplateKeys());
        assertThat(devTemplates.values())
            .allSatisfy(templateId -> assertThat(UUID.fromString(templateId)).isNotNull());
        assertThat(Set.copyOf(devTemplates.values())).hasSameSizeAs(devTemplates.values());
    }

    /**
     * HDPI-7538 leaves only the writ-of-restitution approval TBC ("Remove bit about NICE and advise claimants
     * to contact HCEO"), so that is the one type the claimant hears nothing about (OPEN-QUESTIONS 55).
     */
    @Test
    void shouldHaveAnApprovalEmailForEveryTypeButAWritOfRestitution() {
        assertThat(EnforcementEmail.forApproval(SelectEnforcementType.WARRANT))
            .isEqualTo(EnforcementEmail.WARRANT_APPROVED);
        assertThat(EnforcementEmail.forApproval(SelectEnforcementType.WRIT))
            .isEqualTo(EnforcementEmail.WRIT_APPROVED);
        assertThat(EnforcementEmail.forApproval(SelectEnforcementType.WARRANT_OF_RESTITUTION))
            .isEqualTo(EnforcementEmail.WARRANT_OF_RESTITUTION_APPROVED);
        assertThat(EnforcementEmail.forApproval(SelectEnforcementType.WRIT_OF_RESTITUTION)).isNull();
        assertThat(EnforcementEmail.forApproval(null)).isNull();
    }

    /**
     * HDPI-7538's writ-of-restitution rejection content is a bare "TBC" (OPEN-QUESTIONS 55).
     */
    @Test
    void shouldHaveARejectionEmailForEveryTypeButAWritOfRestitution() {
        assertThat(EnforcementEmail.forRejection(SelectEnforcementType.WARRANT))
            .isEqualTo(EnforcementEmail.WARRANT_REJECTED);
        assertThat(EnforcementEmail.forRejection(SelectEnforcementType.WRIT))
            .isEqualTo(EnforcementEmail.WRIT_REJECTED);
        assertThat(EnforcementEmail.forRejection(SelectEnforcementType.WARRANT_OF_RESTITUTION))
            .isEqualTo(EnforcementEmail.WARRANT_OF_RESTITUTION_REJECTED);
        assertThat(EnforcementEmail.forRejection(SelectEnforcementType.WRIT_OF_RESTITUTION)).isNull();
        assertThat(EnforcementEmail.forRejection(null)).isNull();
    }

    private static List<String> configuredTemplateKeys() {
        return List.copyOf(configuredTemplates().keySet());
    }

    private static Map<String, String> configuredTemplates() {
        return templatesIn("application.yaml");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> templatesIn(String resource) {
        try (InputStream applicationYaml = EnforcementEmailTest.class.getClassLoader()
            .getResourceAsStream(resource)) {
            Map<String, Object> yaml = new Yaml().load(applicationYaml);
            Map<String, Object> enforcementNotify = (Map<String, Object>) yaml.get("enforcement-notify");
            return (Map<String, String>) enforcementNotify.get("templates");
        } catch (Exception ex) {
            throw new IllegalStateException("Could not read enforcement-notify.templates from " + resource, ex);
        }
    }
}
