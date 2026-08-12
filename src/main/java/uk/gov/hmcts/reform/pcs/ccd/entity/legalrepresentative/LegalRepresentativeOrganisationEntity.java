package uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "legal_representative_organisation")
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalRepresentativeOrganisationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String organisationName;

    private String organisationId;

    private String organisationProfileId;

    @OneToOne(mappedBy = "legalRepresentativeOrganisation", cascade = ALL)
    private LegalRepresentativeOrganisationContactDetailsEntity legalRepresentativeOrganisationContactDetails;

    @OneToMany(fetch = LAZY, cascade = ALL, mappedBy = "legalRepresentativeOrganisation")
    @Builder.Default
    @JsonManagedReference
    private List<ClaimPartyLegalRepresentativeOrganisationEntity> claimPartyLegalRepresentativeOrganisationList =
        new ArrayList<>();

    public void addParty(PartyEntity party) {
        if (this.claimPartyLegalRepresentativeOrganisationList.stream().anyMatch(e ->
                                                                         e.getParty().getId().equals(party.getId()))) {
            return;
        }

        ClaimPartyLegalRepresentativeOrganisationEntity claimPartyLegalRepresentativeOrganisationEntity =
            ClaimPartyLegalRepresentativeOrganisationEntity.builder()
            .legalRepresentativeOrganisation(this)
            .party(party)
            .startDate(Instant.now())
            .active(YesOrNo.YES)
            .build();
        claimPartyLegalRepresentativeOrganisationList.add(claimPartyLegalRepresentativeOrganisationEntity);
        party.getPartyLegalRepresentativeOrganisationList().add(claimPartyLegalRepresentativeOrganisationEntity);
    }

    public void setLegalRepresentativeOrganisationContactDetails(
        LegalRepresentativeOrganisationContactDetailsEntity contactDetails) {
        this.legalRepresentativeOrganisationContactDetails = contactDetails;
        if (contactDetails != null) {
            contactDetails.setLegalRepresentativeOrganisation(this);
        }
    }

}
