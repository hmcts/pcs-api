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
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.LAZY;
import static uk.gov.hmcts.ccd.sdk.type.YesOrNo.NO;

@Slf4j
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
        this.claimPartyLegalRepresentativeOrganisationList.stream()
            .filter(e -> e.getParty().getId().equals(party.getId()))
            .findFirst()
            .ifPresentOrElse(claimPartyLegalRepOrgEntity -> {
                if (NO.equals(claimPartyLegalRepOrgEntity.getActive())) {
                    claimPartyLegalRepOrgEntity.setActive(YesOrNo.YES);
                    claimPartyLegalRepOrgEntity.setStartDate(Instant.now());
                } else {
                    log.warn("Party [{}] is already linked to Legal Representative Organisation [{}] and is active.",
                             party.getId(), this.getId());
                }

            }, () -> {
                var claimPartyLegalRepOrgEntity = ClaimPartyLegalRepresentativeOrganisationEntity.builder()
                    .legalRepresentativeOrganisation(this)
                    .party(party)
                    .startDate(Instant.now())
                    .active(YesOrNo.YES)
                    .build();
                claimPartyLegalRepresentativeOrganisationList.add(claimPartyLegalRepOrgEntity);
                party.getClaimPartyLegalRepresentativeOrganisationList().add(claimPartyLegalRepOrgEntity);
            });
    }

    public void setLegalRepresentativeOrganisationContactDetails(
        LegalRepresentativeOrganisationContactDetailsEntity contactDetails) {
        this.legalRepresentativeOrganisationContactDetails = contactDetails;
        if (contactDetails != null) {
            contactDetails.setLegalRepresentativeOrganisation(this);
        }
    }

}
