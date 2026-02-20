package uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum SelectEnforcementType implements HasLabel {

    WARRANT("Warrant of possession"),
    WRIT("Writ of possession"),
    WRIT_OF_RESTITUTION("Writ of restitution");

    private final String label;
}
