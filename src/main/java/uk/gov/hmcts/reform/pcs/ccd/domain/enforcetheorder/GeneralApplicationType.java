package uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum GeneralApplicationType implements HasLabel {

    SUSPENSION("Ask the court to suspend (stop or delay) the eviction, or the warrant on terms"),

    ADJOURNMENT("Ask t adjourn the hearing (move it to a later time or date)"),

    CANCELLATION("Ask the court to set aside (cancel) the decision to evict someone"),

    APPEAL("Appeal"),

    TRANSFER("Transfer your claim from the County Court to the High Court"),

    ELSE("Something else");

    private final String label;
}
