package uk.gov.hmcts.reform.pcs.ccd.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "claim_document")
public class ClaimDocumentEntity {

    @EmbeddedId
    @Builder.Default
    private ClaimDocumentId id = new ClaimDocumentId();

    @ManyToOne
    @MapsId("claimId")
    @JsonBackReference
    private ClaimEntity claim;

    @ManyToOne
    @MapsId("documentId")
    @JsonBackReference
    private DocumentEntity document;

}
