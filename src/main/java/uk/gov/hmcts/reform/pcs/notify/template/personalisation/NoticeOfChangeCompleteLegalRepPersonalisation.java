package uk.gov.hmcts.reform.pcs.notify.template.personalisation;

import lombok.Builder;

import java.util.HashMap;
import java.util.Map;

@Builder
public class NoticeOfChangeCompleteLegalRepPersonalisation implements TemplatePersonalisation {
    private final BasePersonalisation base;
    private final String organisationName;
    private final String partyName;
    private final String nextStepUrl;

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>(base.toMap());
        map.put("organisationName", organisationName);
        map.put("partyName", partyName);
        map.put("nextStepUrl", nextStepUrl);
        return Map.copyOf(map);
    }
}
