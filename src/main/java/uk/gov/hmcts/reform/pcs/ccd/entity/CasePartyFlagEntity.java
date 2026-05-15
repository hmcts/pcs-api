package uk.gov.hmcts.reform.pcs.ccd.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;


@Entity
@Table(name = "case_party_flag")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CasePartyFlagEntity extends BaseCaseFlag {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id")
    private PartyEntity party;

    @Override
    public void setParentEntity(PcsCaseEntity caseEntity, PartyEntity partyEntity) {
        this.party = partyEntity;
    }
}
