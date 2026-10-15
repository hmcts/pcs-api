package uk.gov.hmcts.reform.pcs.hearings.messaging;

import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import uk.gov.hmcts.reform.pcs.hearings.service.HmcHearingService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class HmcServiceBusConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(
            HmcServiceBusConfiguration.class,
            HmcHearingNotificationListener.class,
            HmcHearingNotificationProcessor.class)
        .withBean(ObjectMapper.class, ObjectMapper::new)
        .withBean(HmcHearingService.class, () -> mock(HmcHearingService.class))
        .withPropertyValues("hmc.deployment-id=");

    @Test
    void shouldNotCreateConsumerByDefault() {
        // given
        // when
        // then
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(ServiceBusProcessorClient.class);
            assertThat(context).doesNotHaveBean(HmcHearingNotificationListener.class);
            assertThat(context).doesNotHaveBean(HmcHearingNotificationProcessor.class);
        });
    }

    @Test
    void shouldNotCreateConsumerWhenExplicitlyDisabled() {
        // given
        // when
        // then
        contextRunner.withPropertyValues("hmc.service-bus.enabled=false").run(context ->
            assertThat(context).doesNotHaveBean(ServiceBusProcessorClient.class));
    }

    @Test
    void shouldFailStartupWhenEnabledWithoutConnectionString() {
        // given
        // when
        // then
        contextRunner.withPropertyValues(
            "hmc.service-bus.enabled=true",
            "hmc.service-bus.connection-string=",
            "hmc.service-bus.topic=hmc-to-cft-aat",
            "hmc.service-bus.subscription=hmc-to-pcs-subscription-aat",
            "hmc.service-bus.max-concurrent-calls=1"
        ).run(context -> assertThat(context)
            .hasFailed()
            .getFailure()
            .hasRootCauseInstanceOf(IllegalArgumentException.class));
    }
}
