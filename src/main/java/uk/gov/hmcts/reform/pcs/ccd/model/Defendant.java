package uk.gov.hmcts.reform.pcs.ccd.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;

import java.util.UUID;


@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Defendant {

    private UUID partyId;

    private Boolean nameKnown;

    @CCD
    private String firstName;

    @CCD
    private String lastName;

    private Boolean addressKnown;

    private Boolean addressSameAsPossession;

    private AddressUK correspondenceAddress;

    private Boolean additionalDefendantsAdded; // Only used for auditing

}
