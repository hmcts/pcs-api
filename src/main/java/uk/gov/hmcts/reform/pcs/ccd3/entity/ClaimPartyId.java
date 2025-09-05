package uk.gov.hmcts.reform.pcs.ccd3.entity;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@EqualsAndHashCode
public class ClaimPartyId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private UUID claimId;
    private UUID partyId;

}
