package uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "organisation")
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class OrganisationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String organisationName;

    private String organisationId;

    private String organisationProfileId;

    @OneToMany(mappedBy = "organisation", fetch = LAZY, cascade = ALL)
    @Builder.Default
    @JsonManagedReference
    private List<ClaimPartyContactDetailsEntity> claimPartyContactDetails = new ArrayList<>();

    @OneToMany(fetch = LAZY, cascade = ALL, mappedBy = "organisation")
    @Builder.Default
    @JsonManagedReference
    private List<ClaimPartyOrganisationEntity> claimPartyOrganisationList = new ArrayList<>();

    private LocalDateTime createdDate;

    private LocalDateTime lastModifiedDate;

    public void addParty(PartyEntity party) {
        // Inactive links are history and must not block re-linking after a NoC back to this organisation.
        if (this.claimPartyOrganisationList.stream()
            .filter(e -> e.getActive() == YesOrNo.YES)
            .anyMatch(e -> e.getParty().getId().equals(party.getId()))) {
            log.warn("Party [{}] already has an active link to Legal Representative Organisation [{}], "
                         + "skipping re-link.", party.getId(), this.getId());
            return;
        }

        ClaimPartyOrganisationEntity claimPartyOrganisationEntity =
            ClaimPartyOrganisationEntity.builder()
                .organisation(this)
                .party(party)
                .startDate(Instant.now())
                .active(YesOrNo.YES)
                .build();
        claimPartyOrganisationList.add(claimPartyOrganisationEntity);
        party.getClaimPartyOrganisationList().add(claimPartyOrganisationEntity);
    }

    public void addClaimPartyContactDetails(ClaimPartyContactDetailsEntity contactDetails) {

        if (contactDetails != null) {
            this.claimPartyContactDetails.add(contactDetails);
            contactDetails.setOrganisation(this);
        }
    }

}
