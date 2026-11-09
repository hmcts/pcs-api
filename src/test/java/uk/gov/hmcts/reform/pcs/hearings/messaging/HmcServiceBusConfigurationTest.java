package uk.gov.hmcts.reform.pcs.hearings.messaging;

import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import uk.gov.hmcts.reform.pcs.hearings.service.HmcHearingService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class HmcServiceBusConfigurationTest {

    private static final String CONNECTION_STRING =
        "Endpoint=sb://example.servicebus.windows.net/;SharedAccessKeyName=key;SharedAccessKey=secret";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(
            HmcServiceBusConfiguration.class,
            HmcHearingNotificationListener.class,
            HmcHearingRetrievalHandler.class)
        .withBean(ObjectMapper.class, ObjectMapper::new)
        .withBean(HmcHearingService.class, () -> mock(HmcHearingService.class))
        .withPropertyValues("hmc.deployment-id=", "hmc.serviceId=AAA3");

    @Test
    void shouldNotCreateConsumerByDefault() {
        // given
        // when
        // then
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(ServiceBusProcessorClient.class);
            assertThat(context).doesNotHaveBean(HmcHearingNotificationListener.class);
            assertThat(context).doesNotHaveBean(HmcHearingNotificationHandler.class);
        });
    }

    @Test
    void shouldCreateRetrievalHandlerOnlyWhenConsumerEnabledAndRetrieveOnly() {
        // given
        ApplicationContextRunner handlerRunner = new ApplicationContextRunner()
            .withUserConfiguration(HmcHearingRetrievalHandler.class)
            .withBean(HmcHearingService.class, () -> mock(HmcHearingService.class))
            .withPropertyValues("hmc.deployment-id=deployment-pcs-api-pr-1",
                                "hmc.service-bus.subscription=pcs-api-pr-1-servicebus-pcs");

        // when
        // then
        handlerRunner.withPropertyValues("hmc.service-bus.retrieve-only=true").run(context ->
            assertThat(context).doesNotHaveBean(HmcHearingRetrievalHandler.class));
        handlerRunner.withPropertyValues("hmc.service-bus.enabled=true", "hmc.service-bus.retrieve-only=false")
            .run(context -> assertThat(context).doesNotHaveBean(HmcHearingRetrievalHandler.class));
        handlerRunner.withPropertyValues("hmc.service-bus.enabled=true", "hmc.service-bus.retrieve-only=true")
            .run(context -> assertThat(context).hasSingleBean(HmcHearingRetrievalHandler.class));
    }

    @Test
    void shouldRefuseRetrieveOnlyHandlerWithoutDeploymentId() {
        // given
        ApplicationContextRunner sharedEnvironmentRunner = new ApplicationContextRunner()
            .withUserConfiguration(HmcHearingRetrievalHandler.class)
            .withBean(HmcHearingService.class, () -> mock(HmcHearingService.class))
            .withPropertyValues("hmc.deployment-id=",
                                "hmc.service-bus.subscription=pcs-api-pr-1-servicebus-pcs",
                                "hmc.service-bus.enabled=true",
                                "hmc.service-bus.retrieve-only=true");

        // when
        // then
        sharedEnvironmentRunner.run(context -> assertThat(context)
            .hasFailed()
            .getFailure()
            .hasRootCauseInstanceOf(IllegalStateException.class));
    }

    @Test
    void shouldRefuseRetrieveOnlyHandlerOnSharedSubscription() {
        // given
        ApplicationContextRunner sharedSubscriptionRunner = new ApplicationContextRunner()
            .withUserConfiguration(HmcHearingRetrievalHandler.class)
            .withBean(HmcHearingService.class, () -> mock(HmcHearingService.class))
            .withPropertyValues("hmc.deployment-id=deployment-pcs-api-pr-1",
                                "hmc.service-bus.subscription=hmc-to-pcs-subscription-aat",
                                "hmc.service-bus.enabled=true",
                                "hmc.service-bus.retrieve-only=true");

        // when
        // then
        sharedSubscriptionRunner.run(context -> assertThat(context)
            .hasFailed()
            .getFailure()
            .hasRootCauseInstanceOf(IllegalStateException.class)
            .hasStackTraceContaining("shared subscription"));
    }

    @Test
    void shouldRefuseToStartConsumerWithoutAHandlerThatProcessesNotifications() {
        // given
        // when
        // then
        contextRunner.withPropertyValues(
            "hmc.service-bus.enabled=true",
            "hmc.service-bus.connection-string=" + CONNECTION_STRING,
            "hmc.service-bus.topic=hmc-to-cft-aat",
            "hmc.service-bus.subscription=hmc-to-pcs-subscription-aat",
            "hmc.service-bus.max-concurrent-calls=1"
        ).run(context -> assertThat(context)
            .hasFailed()
            .getFailure()
            .hasRootCauseInstanceOf(NoSuchBeanDefinitionException.class));
    }

    @Test
    void shouldBuildProcessorForConfiguredTopicAndSubscriptionWithoutStartingIt() {
        // given
        HmcHearingNotificationListener listener = mock(HmcHearingNotificationListener.class);

        // when
        ServiceBusProcessorClient client = new HmcServiceBusConfiguration().hmcHearingsProcessorClient(
            CONNECTION_STRING, "hmc-to-cft-aat", "hmc-to-pcs-subscription-aat", 1, listener);

        // then
        try (client) {
            assertThat(client.getTopicName()).isEqualTo("hmc-to-cft-aat");
            assertThat(client.getSubscriptionName()).isEqualTo("hmc-to-pcs-subscription-aat");
            assertThat(client.isRunning()).isFalse();
        }
    }

    @Test
    void shouldFailStartupWhenEnabledWithoutConnectionString() {
        // given
        // when
        // then
        contextRunner.withPropertyValues(
            "hmc.service-bus.enabled=true",
            "hmc.deployment-id=deployment-pcs-api-pr-1",
            "hmc.service-bus.retrieve-only=true",
            "hmc.service-bus.connection-string=",
            "hmc.service-bus.topic=hmc-to-cft-aat",
            "hmc.service-bus.subscription=pcs-api-pr-1-servicebus-pcs",
            "hmc.service-bus.max-concurrent-calls=1"
        ).run(context -> assertThat(context)
            .hasFailed()
            .getFailure()
            .hasRootCauseInstanceOf(IllegalArgumentException.class));
    }
}
