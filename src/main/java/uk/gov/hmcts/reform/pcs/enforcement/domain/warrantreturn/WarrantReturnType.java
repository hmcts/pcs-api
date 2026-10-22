package uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum WarrantReturnType implements HasLabel {

    FINAL("Final"),
    INTERIM("Interim");

    private final String label;
}
