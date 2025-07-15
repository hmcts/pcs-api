package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum UserType {
    @JsonProperty("Claimant")
    CLAIMANT,

    @JsonProperty("Defendant")
    DEFENDANT,

    @JsonProperty("Interested Party")
    INTERESTED_PARTY;
}
