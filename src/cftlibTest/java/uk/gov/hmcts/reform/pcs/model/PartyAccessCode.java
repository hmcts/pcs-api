package uk.gov.hmcts.reform.pcs.model;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public final class PartyAccessCode {

    private final UUID partyId;
    private final String accessCode;

}
