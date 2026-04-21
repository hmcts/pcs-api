package uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "legal_representative")
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalRepresentativeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID idamId;

    @Column(name = "organisation_name")
    private String organisationName;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    private String email;

    private String phone;

    @OneToOne(cascade = ALL,orphanRemoval = true)
    private AddressEntity address;

    @OneToMany(fetch = LAZY, cascade = ALL, mappedBy = "legalRepresentative")
    @Builder.Default
    @JsonManagedReference
    private List<ClaimPartyLegalRepresentativeEntity> claimPartyLegalRepresentativeList = new ArrayList<>();

    public void addParty(PartyEntity party) {
        ClaimPartyLegalRepresentativeEntity claimPartyLegalRepresentativeEntity =
            ClaimPartyLegalRepresentativeEntity.builder()
            .legalRepresentative(this)
            .party(party)
            .startDate(Instant.now())
            .active(VerticalYesNo.YES)
            .build();
        claimPartyLegalRepresentativeList.add(claimPartyLegalRepresentativeEntity);
        party.getClaimPartyLegalRepresentativeList().add(claimPartyLegalRepresentativeEntity);
    }

}
