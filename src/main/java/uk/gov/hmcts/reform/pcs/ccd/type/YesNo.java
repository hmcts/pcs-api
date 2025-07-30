package uk.gov.hmcts.reform.pcs.ccd.type;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum YesNo {
    @JsonProperty("Yes")
    YES,

    @JsonProperty("No")
    NO;
} 