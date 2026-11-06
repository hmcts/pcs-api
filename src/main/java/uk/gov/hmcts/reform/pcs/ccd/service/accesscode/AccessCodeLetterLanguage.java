package uk.gov.hmcts.reform.pcs.ccd.service.accesscode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Language of a defendant access-code letter. A defendant on a Wales case gets one letter in each.
 */
@Getter
@RequiredArgsConstructor
public enum AccessCodeLetterLanguage {

    ENGLISH("English"),
    WELSH("Welsh");

    private final String label;
}
