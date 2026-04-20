package uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative;

import jakarta.persistence.Embeddable;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Data
public class LegalRepresentativePartyId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private UUID partyId;
    private UUID legalRepresentativeId;
}
