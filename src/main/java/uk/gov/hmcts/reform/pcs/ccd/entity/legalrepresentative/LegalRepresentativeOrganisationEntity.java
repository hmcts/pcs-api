package uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String organisationName;

    private String organisationId;

    private String organisationProfileId;

    private String emailAddress;

    private String phoneNumber;

    private String contactReference;

    @OneToOne(cascade = ALL,orphanRemoval = true)
    private AddressEntity address;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private YesOrNo hasAmendedContactDetails;

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

}
