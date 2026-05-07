package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum LanguageUsed implements HasLabel {

    ENGLISH("English"),
    WELSH("Welsh"),
    ENGLISH_AND_WELSH("English and Welsh");

    private final String label;

}
