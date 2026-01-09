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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicence;
import uk.gov.hmcts.reform.pcs.ccd.model.Defendant;
import uk.gov.hmcts.reform.pcs.ccd.model.PartyDocumentDto;
import uk.gov.hmcts.reform.pcs.ccd.model.PossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.model.StatementOfTruth;
import uk.gov.hmcts.reform.pcs.ccd.model.UnderlesseeMortgagee;
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

    @JdbcTypeCode(SqlTypes.JSON)
    private TenancyLicence tenancyLicence;

    @JdbcTypeCode(SqlTypes.JSON)
    private PossessionGrounds possessionGrounds;

    @OneToMany(mappedBy = "pcsCase", fetch = LAZY, cascade = ALL)
    @Builder.Default
    @JsonManagedReference
    private Set<PartyEntity> parties = new HashSet<>();

    @OneToMany(mappedBy = "pcsCase", fetch = LAZY, cascade = ALL)
    @Builder.Default
    @JsonManagedReference
    private List<ClaimEntity> claims = new ArrayList<>();

    @Column(name = "defendant_details")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<Defendant> defendants;

    @Column(name = "party_documents")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<PartyDocumentDto> partyDocuments;

    @Column(name = "statement_of_truth")
    @JdbcTypeCode(SqlTypes.JSON)
    private StatementOfTruth statementOfTruth;

    @Column(name = "underlessee_mortgagee_details")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<UnderlesseeMortgagee> underlesseesMortgagees;

    public void addClaim(ClaimEntity claim) {
        claims.add(claim);
        claim.setPcsCase(this);
    }

    public void addParty(PartyEntity party) {
        parties.add(party);
        party.setPcsCase(this);
    }
}
