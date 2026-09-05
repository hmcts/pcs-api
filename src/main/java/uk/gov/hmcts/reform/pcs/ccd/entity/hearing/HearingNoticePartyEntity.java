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
@Table(name = "hearing_notice_party")
public class HearingNoticePartyEntity {

    @EmbeddedId
    @Builder.Default
    private HearingNoticePartyId id = new HearingNoticePartyId();

    @ManyToOne
    @MapsId("hearingId")
    @JsonBackReference
    private HearingEntity hearing;

    @ManyToOne
    @MapsId("partyId")
    @JsonBackReference
    private PartyEntity party;

    public void removeHearingNoticeParty() {
        party.getHearingNoticeParties().remove(this);
        hearing.getHearingNoticeParties().remove(this);
    }
}
