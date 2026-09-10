package uk.gov.hmcts.reform.pcs.notify.template.personalisation;

import lombok.Builder;

import java.util.HashMap;
import java.util.Map;

@Builder
public class MakeAClaimBasePersonalisation implements TemplatePersonalisation {
    private final TemplatePersonalisation base;
    private final String nextStepUrl;

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>(base.toMap());
        map.put("nextStepUrl", nextStepUrl);
        return Map.copyOf(map);
    }
}
