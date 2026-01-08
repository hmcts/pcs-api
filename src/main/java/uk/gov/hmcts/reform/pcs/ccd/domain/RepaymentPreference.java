package uk.gov.hmcts.reform.pcs.ccd.domain;


import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum RepaymentPreference implements HasLabel {

    ALL("All of it"),
    SOME("Some of it"),
    NONE("None of it");

    private final String label;
}
