package uk.gov.hmcts.reform.pcs.config;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;


@Configuration
@ConfigurationProperties(prefix = "security")
@EnableWebSecurity
public class SecurityConfiguration {

    /**
     * Endpoints served by the CCD SDK that only CCD data store should call: decentralised case
     * persistence (reads and event submission) and the event callbacks (about-to-start, mid-event etc.).
     * They trust the caller to have already enforced CCD access control, so they get their own,
     * narrower S2S allowlist rather than the service-wide one.
     */
    static final String[] CCD_DATA_STORE_ONLY_PATHS = {"/ccd-persistence/**", "/callbacks/**"};

    @Getter
    private final List<String> anonymousPaths = new ArrayList<>();
    private final ServiceAuthFilter serviceAuthFilter;
    private final IdamAuthenticationFilter idamAuthFilter;

    @Autowired
    public SecurityConfiguration(ServiceAuthFilter serviceAuthFilter, IdamAuthenticationFilter idamAuthFilter) {
        super();
        this.serviceAuthFilter = serviceAuthFilter;
        this.idamAuthFilter = idamAuthFilter;
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers(anonymousPaths.toArray(String[]::new));
    }

    @Bean
    @Order(1)
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public SecurityFilterChain ccdDataStoreOnlySecurityFilterChain(
        HttpSecurity http,
        AuthTokenValidator authTokenValidator,
        @Value("${idam.s2s-authorised.ccd-data-store-only-services}") List<String> authorisedServices
    ) throws Exception {

        // Deliberately not a bean: Spring Boot would register a Filter bean as a servlet filter for every request.
        ServiceAuthFilter ccdDataStoreOnlyServiceAuthFilter = new ServiceAuthFilter(authTokenValidator,
                                                                                    authorisedServices);

        http.securityMatcher(CCD_DATA_STORE_ONLY_PATHS);
        return configure(http, ccdDataStoreOnlyServiceAuthFilter);
    }

    @Bean
    @Order(2)
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return configure(http, serviceAuthFilter);
    }

    @SuppressWarnings({"PMD.SignatureDeclareThrowsException", "java:S4502"})
    private SecurityFilterChain configure(HttpSecurity http, ServiceAuthFilter s2sFilter) throws Exception {
        http
            .addFilterBefore(s2sFilter, AbstractPreAuthenticatedProcessingFilter.class)
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
