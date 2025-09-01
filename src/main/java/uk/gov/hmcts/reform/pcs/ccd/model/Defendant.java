package uk.gov.hmcts.reform.pcs.ccd.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;


@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Defendant {

    private String id;

    private Boolean nameKnown;

    private String firstName;

    private String lastName;

    private Boolean addressKnown;

    private Boolean addressSameAsPossession;

    private AddressUK correspondenceAddress;

    private Boolean emailKnown;

    private String email;
}
