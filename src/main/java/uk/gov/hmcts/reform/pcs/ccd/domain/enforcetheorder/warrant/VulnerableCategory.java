package uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum VulnerableCategory implements HasLabel {

    VULNERABLE_ADULTS("Vulnerable adults"),
    VULNERABLE_CHILDREN("Vulnerable children"),
    VULNERABLE_ADULTS_AND_CHILDREN("Vulnerable adults and children");

    private final String label;
}
