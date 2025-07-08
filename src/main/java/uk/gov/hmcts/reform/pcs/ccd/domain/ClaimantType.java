package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@Getter
@RequiredArgsConstructor
public enum ClaimantType implements HasLabel {

    @JsonProperty("Private landlord")
    PRIVATE_LANDLORD("Private landlord", "Private landlord"),

    @JsonProperty("Registered provider of social housing")
    REGISTERED_PROVIDER_OF_SOCIAL_HOUSING("Registered provider of social housing",
                                            "Registered provider of social housing"),


    @JsonProperty("Mortgage provider or lender")
    MORTGAGE_PROVIDER_OR_LENDER("Mortgage provider or lender",
                                            "Mortgage provider or lender"),

    @JsonProperty("Other")
    OTHER("Other",
                                            "Other");

    private final String reason;
    private final String label;

}
