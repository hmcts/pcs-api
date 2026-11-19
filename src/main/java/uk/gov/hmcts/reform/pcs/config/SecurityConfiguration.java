package uk.gov.hmcts.reform.pcs.config;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.pcs.bulkclaim.config.BulkClaimProperties;
import uk.gov.hmcts.reform.pcs.bulkclaim.security.BulkClaimApimS2sFilter;


@Configuration
@ConfigurationProperties(prefix = "security")
@EnableWebSecurity
public class SecurityConfiguration {

    @Getter
    private final List<String> anonymousPaths = new ArrayList<>();
    private final ServiceAuthFilter serviceAuthFilter;
    private final IdamAuthenticationFilter idamAuthFilter;
    private final AuthTokenValidator authTokenValidator;
    private final BulkClaimProperties bulkClaimProperties;

    @Autowired
    public SecurityConfiguration(ServiceAuthFilter serviceAuthFilter,
                                 IdamAuthenticationFilter idamAuthFilter,
                                 @Qualifier("serviceAuthTokenValidator") AuthTokenValidator authTokenValidator,
                                 BulkClaimProperties bulkClaimProperties) {
        super();
        this.serviceAuthFilter = serviceAuthFilter;
        this.idamAuthFilter = idamAuthFilter;
        this.authTokenValidator = authTokenValidator;
        this.bulkClaimProperties = bulkClaimProperties;
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers(anonymousPaths.toArray(String[]::new));
    }

    @Bean
    @SuppressWarnings({"PMD.SignatureDeclareThrowsException", "java:S4502"})
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .addFilterBefore(serviceAuthFilter, AbstractPreAuthenticatedProcessingFilter.class)
            .addFilterBefore(new BulkClaimApimS2sFilter(authTokenValidator, bulkClaimProperties),
                AbstractPreAuthenticatedProcessingFilter.class)
            .addFilterBefore(idamAuthFilter, AbstractPreAuthenticatedProcessingFilter.class)
            .sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(STATELESS))
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            // CSRF protection is not needed: sessions are STATELESS and authentication is via
            // bearer tokens (IDAM/S2S), not cookies, so there is no ambient credential for a
            // cross-site request to exploit. java:S4502 suppressed accordingly.
            .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
