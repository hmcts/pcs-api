package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

import java.util.Arrays;

@AllArgsConstructor
@Getter
public enum SecureOrFlexibleMandatoryGrounds implements HasLabel {

    ANTI_SOCIAL("Antisocial behaviour");

    private final String label;

    public static SecureOrFlexibleMandatoryGrounds fromLabel(String label) {
        return Arrays.stream(values())
            .filter(g -> g.getLabel().equals(label))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No enum constant with label: " + label));
    }
}
