package uk.gov.hmcts.reform.pcs.entity;

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

@Entity
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "general_application_party")
public class GeneralApplicationParty {

    @EmbeddedId
    @Builder.Default
    private ClaimPartyId id = new ClaimPartyId(); // TODO: Borrowed from Claim for simplicity

    @ManyToOne
    @MapsId("claimId")
    @JsonBackReference
    private GeneralApplicationEntity generalApplication;

    @ManyToOne
    @MapsId("partyId")
    @JsonBackReference
    private Party party;

}
