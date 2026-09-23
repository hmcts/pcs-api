package uk.gov.hmcts.reform.pcs.notify.template.personalisation;

import lombok.Builder;

import java.util.HashMap;
import java.util.Map;

@Builder
public class NoticeOfChangeCompletedPersonalisation implements TemplatePersonalisation {
    private final BasePersonalisation base;
    private final String address;

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>(base.toMap());
        map.put("address", address);
        return Map.copyOf(map);
    }
}
