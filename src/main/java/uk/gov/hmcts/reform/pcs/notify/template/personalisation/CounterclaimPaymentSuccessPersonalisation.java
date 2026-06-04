package uk.gov.hmcts.reform.pcs.notify.template.personalisation;

import lombok.Builder;

import java.util.HashMap;
import java.util.Map;

@Builder
public class CounterclaimPaymentSuccessPersonalisation implements TemplatePersonalisation {
    private final DefendantBasePersonalisation base;
    private final String paymentReferenceNumber;

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>(base.toMap());
        map.put("paymentReferenceNumber", paymentReferenceNumber);
        return Map.copyOf(map);
    }
}
