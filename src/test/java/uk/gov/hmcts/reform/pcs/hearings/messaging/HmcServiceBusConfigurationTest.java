package uk.gov.hmcts.reform.pcs.hearings.messaging;

import com.azure.core.http.HttpClient;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.implementation.ServiceBusProcessorClientOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.pcs.hearings.service.HmcHearingService;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class HmcServiceBusConfigurationTest {

    private static final String CONNECTION_STRING =
        "Endpoint=sb://example.servicebus.windows.net/;SharedAccessKeyName=key;SharedAccessKey=secret";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(
            HmcServiceBusConfiguration.class,
            HmcHearingNotificationListener.class,
            HmcHearingRetrievalHandler.class,
            HmcServiceBusActivation.class)
        .withBean(ObjectMapper.class, ObjectMapper::new)
        .withBean(HmcHearingService.class, () -> mock(HmcHearingService.class))
        .withBean(FeatureToggleService.class, () -> mock(FeatureToggleService.class))
        .withPropertyValues("hmc.serviceId=AAA3");

    @Test
    void shouldNotCreateConsumerByDefault() {
        // given
        // when
        // then
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(ServiceBusProcessorClient.class);
            assertThat(context).doesNotHaveBean(HmcHearingNotificationListener.class);
            assertThat(context).doesNotHaveBean(HmcHearingNotificationHandler.class);
            assertThat(context).doesNotHaveBean(HmcServiceBusActivation.class);
        });
    }

    @Test
    void shouldNotCreateConsumerWhenExplicitlyDisabled() {
        // given
        // when
        // then
        contextRunner.withPropertyValues("hmc.service-bus.enabled=false").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(ServiceBusProcessorClient.class);
            assertThat(context).doesNotHaveBean(HmcServiceBusActivation.class);
        });
    }

    @Test
    void shouldCreateConsumerWithoutStartingItWhenEnabledWithValidConfiguration() {
        // given
        // when
        // then
        contextRunner.withPropertyValues(validProperties()).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ServiceBusProcessorClient.class);
            assertThat(context).hasSingleBean(HmcHearingNotificationListener.class);
            assertThat(context).hasSingleBean(HmcHearingRetrievalHandler.class);
            assertThat(context).hasSingleBean(HmcServiceBusActivation.class);
            ServiceBusProcessorClient client = context.getBean(ServiceBusProcessorClient.class);
            assertThat(client.getTopicName()).isEqualTo("hmc-to-cft-aat");
            assertThat(client.getSubscriptionName()).isEqualTo("pcs-api-pr-1-servicebus-pcs");
            assertThat(client.isRunning()).isFalse();
        });
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
        "hmc-to-pcs-subscription-aat",
        "hmc-to-pcs-subscription-demo",
        "hmc-to-pcs-subscription-prod",
        "hmc-to-pcs-subscription-perftest",
        "hmc-to-pcs-subscription-pr-1",
        "pcs-api-pr-1-servicebus-pcs"
    })
    void shouldAcceptPcsSubscriptions(String subscription) {
        // given
        // when
        // then
        contextRunner.withPropertyValues(validProperties())
            .withPropertyValues("hmc.service-bus.subscription=" + subscription, "hmc.deployment-id=")
            .run(context -> {
                assertThat(context).hasNotFailed();
                ServiceBusProcessorClient client = context.getBean(ServiceBusProcessorClient.class);
                assertThat(client.getSubscriptionName()).isEqualTo(subscription);
                assertThat(client.isRunning()).isFalse();
            });
    }

    @Test
    void shouldCreateConsumerWhenDeploymentIdIsNotConfigured() {
        // given
        // when
        // then
        contextRunner.withPropertyValues(validProperties()).withPropertyValues("hmc.deployment-id=").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ServiceBusProcessorClient.class);
            assertThat(context.getBean(ServiceBusProcessorClient.class).isRunning()).isFalse();
        });
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidConfigurations")
    void shouldRefuseToActivateWithInvalidConfiguration(String description, String property, String expectedMessage) {
        // given
        // when
        // then
        contextRunner.withPropertyValues(validProperties()).withPropertyValues(property).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                .rootCause()
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(expectedMessage);
        });
    }

    static Stream<Arguments> invalidConfigurations() {
        return Stream.of(
            Arguments.of("missing connection string", "hmc.service-bus.connection-string=",
                         "hmc.service-bus.connection-string"),
            Arguments.of("missing topic", "hmc.service-bus.topic=", "hmc.service-bus.topic"),
            Arguments.of("missing subscription", "hmc.service-bus.subscription=", "hmc.service-bus.subscription"),
            Arguments.of("another service's AAT subscription",
                         "hmc.service-bus.subscription=hmc-to-civil-subscription-aat", "civil service"),
            Arguments.of("another service's preview subscription",
                         "hmc.service-bus.subscription=hmc-to-sscs-subscription-pr-4896", "sscs service"),
            Arguments.of("another service's hyphenated subscription",
                         "hmc.service-bus.subscription=hmc-to-et-cos-subscription-prod", "et-cos service"),
            Arguments.of("zero concurrency", "hmc.service-bus.max-concurrent-calls=0", "max-concurrent-calls"),
            Arguments.of("negative concurrency", "hmc.service-bus.max-concurrent-calls=-1", "max-concurrent-calls"),
            Arguments.of("zero delivery count", "hmc.service-bus.max-delivery-count=0", "max-delivery-count")
        );
    }

    @Test
    void shouldRefuseToActivateWhenConnectionStringIsMalformed() {
        // given
        // when
        // then
        contextRunner.withPropertyValues(validProperties())
            .withPropertyValues("hmc.service-bus.connection-string=not-a-connection-string")
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure()).rootCause().isInstanceOf(IllegalArgumentException.class);
            });
    }

    @Test
    void shouldBuildProcessorForConfiguredTopicAndSubscriptionWithoutStartingIt() {
        // given
        HmcHearingNotificationListener listener = mock(HmcHearingNotificationListener.class);

        // when
        ServiceBusProcessorClient client = new HmcServiceBusConfiguration().hmcHearingsProcessorClient(
            CONNECTION_STRING, "hmc-to-cft-aat", "pcs-api-pr-1-servicebus-pcs", 1, 10, "deployment-pcs-api-pr-1",
            listener);

        // then
        try (client) {
            assertThat(client.getTopicName()).isEqualTo("hmc-to-cft-aat");
            assertThat(client.getSubscriptionName()).isEqualTo("pcs-api-pr-1-servicebus-pcs");
            assertThat(client.isRunning()).isFalse();
        }
    }

    @Test
    void shouldBuildProcessorWithAutoCompleteDisabledSoThatListenerControlsSettlement() {
        // given
        HmcHearingNotificationListener listener = mock(HmcHearingNotificationListener.class);

        // when
        ServiceBusProcessorClient client = new HmcServiceBusConfiguration().hmcHearingsProcessorClient(
            CONNECTION_STRING, "hmc-to-cft-aat", "pcs-api-pr-1-servicebus-pcs", 3, 10, "deployment-pcs-api-pr-1",
            listener);

        // then
        try (client) {
            ServiceBusProcessorClientOptions options =
                (ServiceBusProcessorClientOptions) ReflectionTestUtils.getField(client, "processorOptions");
            assertThat(options).isNotNull();
            assertThat(options.isDisableAutoComplete()).isTrue();
            assertThat(options.getMaxConcurrentCalls()).isEqualTo(3);
        }
    }

    @Test
    void shouldUseJdkHttpClientBecauseNettyClientIsExcluded() {
        // given
        // when
        HttpClient httpClient = HttpClient.createDefault();

        // then
        assertThat(httpClient.getClass().getName()).isEqualTo("com.azure.core.http.jdk.httpclient.JdkHttpClient");
        assertThatThrownBy(() -> Class.forName("com.azure.core.http.netty.NettyAsyncHttpClientProvider"))
            .isInstanceOf(ClassNotFoundException.class);
    }

    private static String[] validProperties() {
        return new String[] {
            "hmc.service-bus.enabled=true",
            "hmc.deployment-id=deployment-pcs-api-pr-1",
            "hmc.service-bus.connection-string=" + CONNECTION_STRING,
            "hmc.service-bus.topic=hmc-to-cft-aat",
            "hmc.service-bus.subscription=pcs-api-pr-1-servicebus-pcs",
            "hmc.service-bus.max-concurrent-calls=1",
            "hmc.service-bus.max-delivery-count=10"
        };
    }
}
