package uk.gov.hmcts.reform.pcs.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@ConfigurationProperties(prefix = "spring.jpa")
@Getter
@Setter
public class JpaPropertiesConfig {

    private Map<String, String> properties = new HashMap<>();
    private String hibernateGenerateDdl;

    public Properties toProperties() {
        Properties props = new Properties();
        props.putAll(properties);
        return props;
    }
}
