package uk.gov.hmcts.reform.pcs.config;

import org.apache.commons.logging.Log;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

/**
 * Populates the {@code ccd.decentralised-runtime.system-user} identity by resolving it from the
 * system user's own IDAM credentials before the application context starts, so the id and names in
 * the case event audit always match the live IDAM account. Setting PCS_IDAM_SYSTEM_USER_ID skips
 * the lookup, and a failed lookup falls back to the configured defaults with a warning.
 */
public class SystemUserEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private final Log log;
    private final SystemUserIdentityResolver resolver;

    public SystemUserEnvironmentPostProcessor(DeferredLogFactory logFactory) {
        this(logFactory, new SystemUserIdentityResolver());
    }

    SystemUserEnvironmentPostProcessor(DeferredLogFactory logFactory, SystemUserIdentityResolver resolver) {
        this.log = logFactory.getLog(SystemUserEnvironmentPostProcessor.class);
        this.resolver = resolver;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.containsProperty("PCS_IDAM_SYSTEM_USER_ID")) {
            log.info("System user id supplied explicitly; skipping IDAM resolution");
            return;
        }

        try {
            SystemUserIdentityResolver.SystemUserIdentity identity = resolver.resolve(
                environment.getProperty("IDAM_API_URL", "http://localhost:5062"),
                environment.getProperty("PCS_IDAM_SYSTEM_USERNAME", "pcs-system-user@localhost"),
                environment.getProperty("PCS_IDAM_SYSTEM_PASSWORD", "password"),
                environment.getProperty("IDAM_CLIENT_ID", "pcs-api"),
                environment.getProperty("IDAM_CLIENT_SECRET", "")
            );
            environment.getPropertySources().addFirst(new MapPropertySource(
                "resolvedSystemUserIdentity",
                Map.of(
                    "ccd.decentralised-runtime.system-user.id", identity.id(),
                    "ccd.decentralised-runtime.system-user.first-name", nonBlankOr(identity.firstName(), "Service"),
                    "ccd.decentralised-runtime.system-user.last-name", nonBlankOr(identity.lastName(), "Account")
                )
            ));
            log.info("Resolved system user identity from IDAM: " + identity.id());
        } catch (Exception e) {
            log.warn("Could not resolve system user identity from IDAM; using configured defaults", e);
        }
    }

    private String nonBlankOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER + 10;
    }
}
