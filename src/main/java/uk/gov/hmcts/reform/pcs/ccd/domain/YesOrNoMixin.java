package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A Jackson Mixin that can be used to serialise the CCD SDK {@link uk.gov.hmcts.ccd.sdk.type.YesOrNo}
 * enum in upper case, rather than title case which is how it is configured in the enum.
 */
public enum YesOrNoMixin {
    @JsonProperty("YES")
    YES,
    @JsonProperty("NO")
    NO
}
