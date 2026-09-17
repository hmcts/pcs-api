package uk.gov.hmcts.reform.pcs.testingsupport.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartyEmail {

    private UUID partyId;
    private String emailAddress;

}
