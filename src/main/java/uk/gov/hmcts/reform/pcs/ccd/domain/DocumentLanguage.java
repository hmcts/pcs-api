package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The language a single document is written in. Unlike {@link LanguageUsed}, which records how someone answered a
 * form, a document is only ever in one language.
 */
@Getter
@RequiredArgsConstructor
public enum DocumentLanguage {

    ENGLISH("English"),
    WELSH("Welsh");

    private final String label;
}
