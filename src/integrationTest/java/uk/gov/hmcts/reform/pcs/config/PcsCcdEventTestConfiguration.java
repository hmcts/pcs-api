package uk.gov.hmcts.reform.pcs.config;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.PostgreSQLContainer;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport;
import uk.gov.hmcts.ccd.sdk.testing.TestActors;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.am.RoleAssignmentApi;
import uk.gov.hmcts.reform.pcs.idam.IdamUserInfoApi;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.reference.api.RdProfessionalApi;

import java.time.Instant;
import java.util.List;

/** Fakes for the services pcs-api calls while handling a CCD event. */
@TestConfiguration(proxyBeanMethods = false)
public class PcsCcdEventTestConfiguration {

    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine").withCommand("postgres", "-c", "max_connections=300");

    static {
        POSTGRES.start();
    }

    @Bean
    DynamicPropertyRegistrar postgresProperties() {
        return registry -> {
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES::getUsername);
            registry.add("spring.datasource.password", POSTGRES::getPassword);
            registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        };
    }

    /** Feign registers its clients as primary; the fakes below take precedence over them. */
    @Bean
    static BeanDefinitionRegistryPostProcessor demoteFakedFeignClients() {
        return new BeanDefinitionRegistryPostProcessor() {
            @Override
            public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
                List<Class<?>> clients =
                    List.of(IdamUserInfoApi.class, RoleAssignmentApi.class, RdProfessionalApi.class);
                for (Class<?> client : clients) {
                    if (registry.containsBeanDefinition(client.getName())) {
                        registry.getBeanDefinition(client.getName()).setPrimary(false);
                    }
                }
            }
        };
    }

    /** Answers pcs-api's IDAM lookup for every actor the SDK test support registers. */
    @Bean
    @Primary
    IdamUserInfoApi testActorUserInfoApi(TestActors actors) {
        return bearerToken -> {
            if (CcdEventTestSupport.DEFAULT_AUTHORISATION.equals(bearerToken)) {
                return UserInfo.builder().uid("ccd-sdk-test").sub("ccd-sdk-test").name("SDK Test User")
                    .givenName("SDK").familyName("Test User").roles(List.of("caseworker")).build();
            }
            var actor = actors.require(bearerToken);
            return UserInfo.builder()
                .uid(actor.uid())
                .sub(actor.email())
                .name(actor.givenName() + " " + actor.familyName())
                .givenName(actor.givenName())
                .familyName(actor.familyName())
                .roles(actor.roles())
                .build();
        };
    }

    @Bean
    @Primary
    TestOrganisations testOrganisations() {
        return new TestOrganisations();
    }

    @Bean
    @Primary
    AuthTokenGenerator testServiceTokenGenerator() {
        return () -> "Bearer pcs-api-test-s2s";
    }

    /** Issues a fixed token for pcs-api's own service identities, such as the PRD admin user. */
    @Bean
    @Primary
    OAuth2AuthorizedClientManager testAuthorizedClientManager() {
        return request -> new OAuth2AuthorizedClient(
            ClientRegistration.withRegistrationId(request.getClientRegistrationId())
                .clientId("pcs-api-test")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenUri("https://idam.test/o/token")
                .build(),
            request.getPrincipal().getName(),
            new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
                "pcs-api-test-" + request.getClientRegistrationId(), Instant.now(), Instant.now().plusSeconds(3600)));
    }
}
