package uk.gov.hmcts.reform.pcs.ccd.entity;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@EqualsAndHashCode
public class ClaimDocumentId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private UUID claimId;
    private UUID documentId;
}
