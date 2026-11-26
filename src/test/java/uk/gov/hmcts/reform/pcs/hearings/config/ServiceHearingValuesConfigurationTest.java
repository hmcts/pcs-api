package uk.gov.hmcts.reform.pcs.hearings.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.pcs.hearings.model.Navigation;
import uk.gov.hmcts.reform.pcs.hearings.model.ScreenNavigation;
import uk.gov.hmcts.reform.pcs.hearings.model.ServiceHearingValues;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
    classes = ServiceHearingValuesConfigurationTest.TestConfiguration.class,
    initializers = ConfigDataApplicationContextInitializer.class
)
class ServiceHearingValuesConfigurationTest {

    @Autowired
    private ServiceHearingValuesConfiguration underTest;

    @Configuration
    @EnableConfigurationProperties(ServiceHearingValuesConfiguration.class)
    static class TestConfiguration {
    }

    @Test
    void shouldBuildServiceHearingValuesUsingApplicationYamlDefaults() {

        ServiceHearingValues result = underTest.getDefaultValues();

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
            .isEqualTo("court");
        assertThat(result.getPanelRequirements().getRoleType()).containsExactly("24", "45");
        assertThat(result.getParties())
            .singleElement()
            .extracting(party -> party.getIndividualDetails().getPreferredHearingChannel())
            .isEqualTo("INTER");
    }

    @Test
    void shouldBuildScreenFlowInRequestHearingOrder() {
        List<ScreenNavigation> screenFlow = underTest.getDefaultValues().getScreenFlow();

        assertThat(screenFlow)
            .extracting(ScreenNavigation::getScreenName)
            .containsExactly(
                "hearing-panel", "hearing-facilities", "hearing-stage", "hearing-attendance", "hearing-venue",
                "hearing-welsh", "hearing-judge", "hearing-timing", "hearing-link", "hearing-additional-instructions");
        assertThat(screenFlow.getLast().getNavigation()).isNull();
        assertThat(screenFlow)
            .filteredOn(screen -> !"hearing-venue".equals(screen.getScreenName()))
            .allSatisfy(screen -> assertThat(screen.getConditionKey()).isNull());
        assertThat(screenFlow.get(5).getNavigation())
            .singleElement()
            .extracting(Navigation::getResultValue)
            .isEqualTo("hearing-judge");
    }

    @Test
    void shouldShowWelshScreenAfterVenueOnlyForWalesRegion() {
        ScreenNavigation venue = underTest.getDefaultValues().getScreenFlow().stream()
            .filter(screen -> "hearing-venue".equals(screen.getScreenName()))
            .findFirst()
            .orElseThrow();

        assertThat(venue.getConditionKey()).isEqualTo("regionId");
        assertThat(venue.getNavigation())
            .extracting(Navigation::getConditionOperator, Navigation::getConditionValue, Navigation::getResultValue)
            .containsExactly(
                tuple("INCLUDE", "7", "hearing-welsh"),
                tuple("NOT INCLUDE", "7", "hearing-judge"));
    }

    @Nested
    @TestPropertySource(properties = {
        "HEARINGS_HMCTS_SERVICE_ID=BBA3",
        "HEARINGS_AUTO_LIST_FLAG=true",
        "HEARINGS_ROLE_TYPES=10,20,30",
        "HEARINGS_HEARING_CHANNELS=INTER,VID",
        "HEARINGS_NUMBER_OF_DAYS_OFFSET=10"
    })
    class WithEnvironmentOverrides {

        @Autowired
        private ServiceHearingValuesConfiguration overridden;

        @Test
        void shouldBuildServiceHearingValuesUsingEnvironmentOverrides() {
            ServiceHearingValues result = overridden.getDefaultValues();

            assertThat(result.getHmctsServiceID()).isEqualTo("BBA3");
            assertThat(result.isAutoListFlag()).isTrue();
            assertThat(result.getPanelRequirements().getRoleType()).containsExactly("10", "20", "30");
            assertThat(result.getHearingChannels()).containsExactly("INTER", "VID");
            assertThat(overridden.getNumberOfDaysOffset()).isEqualTo(10);
            assertThat(result.getHearingType()).isEqualTo("AAA3-FPH");
        }
    }
}
