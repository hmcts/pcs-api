package uk.gov.hmcts.reform.pcs.testingsupport.model;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class PartyEmail {

    private final UUID partyId;
    private final String emailAddress;

}
