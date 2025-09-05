package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum SecureOrFlexibleMandatoryGrounds implements HasLabel {

    ANTI_SOCIAL("Antisocial behaviour");

    private final String label;

}
