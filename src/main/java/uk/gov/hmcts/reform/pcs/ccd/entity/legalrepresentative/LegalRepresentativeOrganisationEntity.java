package uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "legal_representative_org")
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalRepresentativeOrganisationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "case_id")
    @JsonBackReference
    private PcsCaseEntity pcsCase;

    private String organisationName;

    private String organisationId;

    private String email;

    private String phone;

    private String contactReference;

    @OneToOne(cascade = ALL,orphanRemoval = true)
    private AddressEntity address;

    @OneToMany(fetch = LAZY, cascade = ALL, mappedBy = "legalRepresentativeOrganisation")
    @Builder.Default
    @JsonManagedReference
    private List<PartyLegalRepresentativeOrganisationEntity> partyLegalRepresentativeOrganisationList =
        new ArrayList<>();

    @OneToMany(fetch = LAZY, cascade = ALL, mappedBy = "legalRepresentative")
    @Builder.Default
    @JsonManagedReference
    private List<LegalRepresentativeEntity> legalRepresentativeList = new ArrayList<>();

    public void addParty(PartyEntity party) {
        if (this.partyLegalRepresentativeOrganisationList.stream().anyMatch(e ->
                                                                         e.getParty().getId().equals(party.getId()))) {
            return;
        }

        PartyLegalRepresentativeOrganisationEntity partyLegalRepresentativeOrganisationEntity =
            PartyLegalRepresentativeOrganisationEntity.builder()
            .legalRepresentativeOrganisation(this)
            .party(party)
            .startDate(Instant.now())
            .active(YesOrNo.YES)
            .build();
        partyLegalRepresentativeOrganisationList.add(partyLegalRepresentativeOrganisationEntity);
        party.getClaimPartyLegalRepresentativeList().add(partyLegalRepresentativeOrganisationEntity);
    }

    public void addLegalRepresentative(LegalRepresentativeEntity legalRepresentative) {
        legalRepresentativeList.add(legalRepresentative);
        legalRepresentative.setLegalRepresentativeOrganisation(this);
    }

}
