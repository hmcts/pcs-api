package uk.gov.hmcts.reform.pcs.ccd.entity.hearing;

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
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "hearing_party")
public class HearingPartyEntity {

    @EmbeddedId
    @Builder.Default
    private HearingPartyId id = new HearingPartyId();

    @ManyToOne
    @MapsId("hearingId")
    @JsonBackReference
    private HearingEntity hearing;

    @ManyToOne
    @MapsId("partyId")
    @JsonBackReference
    private PartyEntity party;
}
