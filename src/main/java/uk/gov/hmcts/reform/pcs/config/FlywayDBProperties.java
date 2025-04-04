package uk.gov.hmcts.reform.pcs.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "spring.flyway.datasource")
@Data
public class FlywayDBProperties {
    private String url;
    private String username;
    private String password;
    private String dialect;
    private String driverClassName;
    private String locations;
}
