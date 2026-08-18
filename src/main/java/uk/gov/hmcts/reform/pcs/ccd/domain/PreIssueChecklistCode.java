package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum PreIssueChecklistCode implements HasLabel {

    TRANSLATE_DEFENDANT_DOCUMENT("Translate defendant document");

    private final String label;

}