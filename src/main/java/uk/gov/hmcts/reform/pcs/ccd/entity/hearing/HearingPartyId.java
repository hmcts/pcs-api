package uk.gov.hmcts.reform.pcs.ccd.entity.hearing;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Data
public class HearingPartyId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer hearingId;

    private UUID partyId;
}
