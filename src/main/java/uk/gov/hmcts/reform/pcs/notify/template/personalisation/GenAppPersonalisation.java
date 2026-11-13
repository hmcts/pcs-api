package uk.gov.hmcts.reform.pcs.notify.template.personalisation;

import lombok.Builder;

import java.util.HashMap;
import java.util.Map;

@Builder
public class GenAppPersonalisation implements TemplatePersonalisation {
    private final BasePersonalisation base;
    private final String subjectLine;
    private final String applicationTypePhrase;
    private final String signInUrl;

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>(base.toMap());
        map.put("subjectLine", subjectLine);
        map.put("applicationTypePhrase", applicationTypePhrase);
        map.put("signInUrl", signInUrl);
        return Map.copyOf(map);
    }
}
