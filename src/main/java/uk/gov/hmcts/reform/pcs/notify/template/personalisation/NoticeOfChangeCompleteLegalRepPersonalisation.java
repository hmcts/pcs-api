package uk.gov.hmcts.reform.pcs.notify.template.personalisation;

import lombok.Builder;

import java.util.HashMap;
import java.util.Map;

@Builder
public class NoticeOfChangeCompleteLegalRepPersonalisation implements TemplatePersonalisation {
    private final BasePersonalisation base;
    private final String organisationName;
    private final String partyName;

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>(base.toMap());
        map.put("organisationName", organisationName);
        map.put("partyName", partyName);
        return Map.copyOf(map);
    }
}
