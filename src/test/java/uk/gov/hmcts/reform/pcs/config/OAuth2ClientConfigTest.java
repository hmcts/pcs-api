package uk.gov.hmcts.reform.pcs.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class OAuth2ClientConfigTest {

    @Mock
    private ClientRegistrationRepository clientRegistrationRepository;

    @Mock
    private OAuth2AuthorizedClientService authorizedClientService;

    private OAuth2AuthorizedClientManager authorizedClientManager;

    @BeforeEach
    void setUp() {
        OAuth2ClientConfig config = new OAuth2ClientConfig();
        authorizedClientManager = config.authorizedClientManager(
            clientRegistrationRepository,
            authorizedClientService
        );
    }

    @Test
    void shouldCreateAuthorizedClientManagerBean() {
        assertThat(authorizedClientManager).isNotNull();
    }

    @Test
    void contextAttributesMapperShouldCopyUsernameAndPasswordWhenBothPresent() {
        Function<OAuth2AuthorizeRequest, Map<String, Object>> mapper = extractMapper();

        OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest
            .withClientRegistrationId("system-user")
            .principal("system-user")
            .attribute(OAuth2ParameterNames.USERNAME, "alice")
            .attribute(OAuth2ParameterNames.PASSWORD, "s3cret")
            .build();

        Map<String, Object> attrs = mapper.apply(request);

        assertThat(attrs)
            .containsEntry(OAuth2AuthorizationContext.USERNAME_ATTRIBUTE_NAME, "alice")
            .containsEntry(OAuth2AuthorizationContext.PASSWORD_ATTRIBUTE_NAME, "s3cret")
            .hasSize(2);
    }

    @Test
    void contextAttributesMapperShouldReturnEmptyMapWhenUsernameMissing() {
        Function<OAuth2AuthorizeRequest, Map<String, Object>> mapper = extractMapper();

        OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest
            .withClientRegistrationId("system-user")
            .principal("system-user")
            .attribute(OAuth2ParameterNames.PASSWORD, "s3cret")
            .build();

        Map<String, Object> attrs = mapper.apply(request);

        assertThat(attrs).isEmpty();
    }

    @Test
    void contextAttributesMapperShouldReturnEmptyMapWhenPasswordMissing() {
        Function<OAuth2AuthorizeRequest, Map<String, Object>> mapper = extractMapper();

        OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest
            .withClientRegistrationId("system-user")
            .principal("system-user")
            .attribute(OAuth2ParameterNames.USERNAME, "alice")
            .build();

        Map<String, Object> attrs = mapper.apply(request);

        assertThat(attrs).isEmpty();
    }

    @Test
    void contextAttributesMapperShouldReturnEmptyMapWhenUsernameBlank() {
        Function<OAuth2AuthorizeRequest, Map<String, Object>> mapper = extractMapper();

        OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest
            .withClientRegistrationId("system-user")
            .principal("system-user")
            .attribute(OAuth2ParameterNames.USERNAME, "   ")
            .attribute(OAuth2ParameterNames.PASSWORD, "s3cret")
            .build();

        Map<String, Object> attrs = mapper.apply(request);

        assertThat(attrs).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private Function<OAuth2AuthorizeRequest, Map<String, Object>> extractMapper() {
        Function<OAuth2AuthorizeRequest, Map<String, Object>> mapper =
            (Function<OAuth2AuthorizeRequest, Map<String, Object>>)
                ReflectionTestUtils.getField(authorizedClientManager, "contextAttributesMapper");
        assertThat(mapper).isNotNull();
        return mapper;
    }
}
