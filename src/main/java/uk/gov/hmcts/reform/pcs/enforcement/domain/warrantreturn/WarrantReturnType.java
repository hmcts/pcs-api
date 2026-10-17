package uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

/**
 * The "Return" question of "Record warrant return" (HDPI-7347). A final return records how the warrant
 * ended; an interim return records something that happened along the way.
 */
@AllArgsConstructor
@Getter
public enum WarrantReturnType implements HasLabel {

    FINAL("Final"),
    INTERIM("Interim");

    private final String label;
}
