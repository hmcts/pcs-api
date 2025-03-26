package uk.gov.hmcts.reform.pcs.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "claim_party")
public class ClaimParty {

    @EmbeddedId
    @Builder.Default
    private ClaimPartyId id = new ClaimPartyId();

    @ManyToOne
    @MapsId("claimId")
    @JsonBackReference
    private Claim claim;

    @ManyToOne
    @MapsId("partyId")
    @JsonBackReference
    private Party party;

    @Enumerated(EnumType.STRING)
    private PartyRole role;

}
