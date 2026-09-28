package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum TranslatedLanguage implements HasLabel {

    ENGLISH("English"),
    WELSH("Welsh");

    private final String label;

}
