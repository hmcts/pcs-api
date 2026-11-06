package uk.gov.hmcts.reform.pcs.ccd.service.accesscode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AccessCodeLetterLanguage {

    ENGLISH("English"),
    WELSH("Welsh");

    private final String label;
}
