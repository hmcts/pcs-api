package uk.gov.hmcts.reform.pcs.hearings.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.pcs.hearings.model.ServiceHearingValues;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
    classes = ServiceHearingValuesConfigurationTest.TestConfiguration.class,
    initializers = ConfigDataApplicationContextInitializer.class
)
class ServiceHearingValuesConfigurationTest {

    private static final String CASE_ID = "1234567890123456";

    @Autowired
    private ServiceHearingValuesConfiguration underTest;

    @Configuration
    @EnableConfigurationProperties(ServiceHearingValuesConfiguration.class)
    static class TestConfiguration {
    }

    @Test
    void shouldLoadDefaultValuesFromApplicationYaml() {
        assertThat(underTest.getHmctsServiceId()).isEqualTo("AAA3");
        assertThat(underTest.isCaseAdditionalSecurityFlag()).isFalse();
        assertThat(underTest.isCaseRestrictedFlag()).isFalse();
        assertThat(underTest.isAutoListFlag()).isFalse();
        assertThat(underTest.isHearingInWelshFlag()).isFalse();
        assertThat(underTest.isPrivateHearingRequiredFlag()).isFalse();
        assertThat(underTest.isHearingIsLinkedFlag()).isFalse();
        assertThat(underTest.getLocationType()).isEqualTo("Court");
        assertThat(underTest.getRoleTypes()).containsExactly("24", "45");
        assertThat(underTest.getPreferredHearingChannel()).isEqualTo("INTER");
        assertThat(underTest.getNumberOfDaysOffset()).isEqualTo(57);
    }

    @Test
    void shouldBuildServiceHearingValuesUsingApplicationYamlDefaults() {

        ServiceHearingValues result = underTest.getDefaultValues(CASE_ID);

        assertThat(result.getCaseId()).isEqualTo(CASE_ID);
        assertThat(result.getHmctsServiceID()).isEqualTo("AAA3");
        assertThat(result.getCaseAdditionalSecurityFlag()).isFalse();
        assertThat(result.getCaseRestrictedFlag()).isFalse();
        assertThat(result.isAutoListFlag()).isFalse();
        assertThat(result.isHearingInWelshFlag()).isFalse();
        assertThat(result.isPrivateHearingRequiredFlag()).isFalse();
        assertThat(result.isHearingIsLinkedFlag()).isFalse();
        assertThat(result.getHearingLocations())
            .singleElement()
            .extracting("locationType")
            .isEqualTo("Court");
        assertThat(result.getPanelRequirements().getRoleType()).containsExactly("24", "45");
        assertThat(result.getParties())
            .singleElement()
            .extracting(party -> party.getIndividualDetails().getPreferredHearingChannel())
            .isEqualTo("INTER");
    }
}
