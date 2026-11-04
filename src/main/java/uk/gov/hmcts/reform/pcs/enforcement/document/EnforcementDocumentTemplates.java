package uk.gov.hmcts.reform.pcs.enforcement.document;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "enforcement.documents")
public class EnforcementDocumentTemplates {

    private Map<String, String> templates = new LinkedHashMap<>();

    public Optional<String> templateId(EnforcementOutput output) {
        if (output == null || templates == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(templates.get(output.getTemplateKey())).filter(id -> isNotBlank(id));
    }
}
