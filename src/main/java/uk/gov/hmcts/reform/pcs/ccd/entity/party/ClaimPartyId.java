package uk.gov.hmcts.reform.pcs.ccd.entity.party;

import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Data
@EqualsAndHashCode
public class ClaimPartyId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private UUID claimId;
    private UUID partyId;

}
