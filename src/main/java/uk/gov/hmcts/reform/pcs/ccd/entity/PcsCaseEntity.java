package uk.gov.hmcts.reform.pcs.ccd.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Column;
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
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "pcs_case")
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PcsCaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private Long caseReference;

    @OneToOne(cascade = ALL,orphanRemoval = true)
    private AddressEntity propertyAddress;

    @Enumerated(EnumType.STRING)
    private LegislativeCountry legislativeCountry;

    @Enumerated(EnumType.STRING)
    @Column(name = "claimant_type")
    private ClaimantType claimantType;

    private Integer caseManagementLocation;

    private Boolean preActionProtocolCompleted;

    @OneToOne(mappedBy = "pcsCase", cascade = ALL, orphanRemoval = true)
    @JsonManagedReference
    private TenancyLicenceEntity tenancyLicence;

    @OneToMany(mappedBy = "pcsCase", fetch = LAZY, cascade = ALL)
    @Builder.Default
    @JsonManagedReference
    private Set<PartyEntity> parties = new HashSet<>();

    @OneToMany(mappedBy = "pcsCase", fetch = LAZY, cascade = ALL)
    @Builder.Default
    @JsonManagedReference
    private List<ClaimEntity> claims = new ArrayList<>();

    @OneToMany(mappedBy = "pcsCase", fetch = LAZY, cascade = ALL, orphanRemoval = true)
    @Builder.Default
    @JsonManagedReference
    private List<DocumentEntity> documents = new ArrayList<>();

    public void setTenancyLicence(TenancyLicenceEntity tenancyLicence) {
        if (this.tenancyLicence != null) {
            this.tenancyLicence.setPcsCase(null);
        }

        this.tenancyLicence = tenancyLicence;

        if (this.tenancyLicence != null) {
            this.tenancyLicence.setPcsCase(this);
        }
    }

    public void addClaim(ClaimEntity claim) {
        claims.add(claim);
        claim.setPcsCase(this);
    }

    public void addParty(PartyEntity party) {
        parties.add(party);
        party.setPcsCase(this);
    }

    public void addDocuments(List<DocumentEntity> documents) {
        for (DocumentEntity document : documents) {
            document.setPcsCase(this);
            this.documents.add(document);
        }
    }
}
