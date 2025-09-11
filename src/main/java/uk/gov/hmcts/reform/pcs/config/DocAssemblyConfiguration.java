package uk.gov.hmcts.reform.pcs.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "doc-assembly")
@Data
public class DocAssemblyConfiguration {
    private String url;
    private Map<String, String> templates = new HashMap<>();
}
