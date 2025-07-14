package uk.gov.hmcts.reform.pcs.ccd.domain;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PartyType {
    @JsonProperty("Claimant")
    CLAIMANT("Claimant"),

    @JsonProperty("Defendant")
    DEFENDANT("Defendant"),

    @JsonProperty("Interested Party")
    INTERESTED_PARTY("Interested Party");

    private final String label;
    }
