package uk.gov.hmcts.reform.pcs.ccd.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.HousingActWalesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.NoticeOfPossessionEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.PossessionAlternativesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.RentArrearsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.LAZY;

/**
 * JPA Entity representing a claim in a case.
 */
@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "claim")
public class ClaimEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "case_id")
    @JsonBackReference
    private PcsCaseEntity pcsCase;

    @Enumerated(EnumType.STRING)
    private ClaimantType claimantType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo againstTrespassers;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private YesOrNo dueToRentArrears;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo claimCosts;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo preActionProtocolFollowed;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo mediationAttempted;

    private String mediationDetails;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo settlementAttempted;

    private String settlementDetails;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo claimantCircumstancesProvided;

    private String claimantCircumstances;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo additionalDefendants;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo defendantCircumstancesProvided;

    private String defendantCircumstances;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo additionalReasonsProvided;

    private String additionalReasons;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo underlesseeOrMortgagee;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo additionalUnderlesseesOrMortgagees;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo additionalDocsProvided;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo genAppExpected;

    @Enumerated(EnumType.STRING)
    private LanguageUsed languageUsed;

    @OneToMany(fetch = LAZY, cascade = ALL, mappedBy = "claim")
    @Builder.Default
    @JsonManagedReference
    private List<ClaimPartyEntity> claimParties = new ArrayList<>();

    @OneToMany(fetch = LAZY, cascade = ALL, mappedBy = "claim")
    @Builder.Default
    @JsonManagedReference
    private Set<ClaimGroundEntity> claimGrounds = new HashSet<>();

    @OneToMany(fetch = LAZY, cascade = ALL, mappedBy = "claim")
    @Builder.Default
    @JsonManagedReference
    private List<ClaimDocumentEntity> claimDocuments = new ArrayList<>();

    @OneToMany(fetch = LAZY, cascade = ALL, mappedBy = "claim")
    @Builder.Default
    @JsonManagedReference
    private Set<EnforcementOrderEntity> enforcementOrders = new HashSet<>();

    @OneToOne(cascade = ALL, mappedBy = "claim", orphanRemoval = true)
    @JsonManagedReference
    private HousingActWalesEntity housingActWales;

    @OneToOne(cascade = ALL, mappedBy = "claim", orphanRemoval = true)
    @JsonManagedReference
    private AsbProhibitedConductEntity asbProhibitedConductEntity;

    @OneToOne(cascade = ALL, mappedBy = "claim", orphanRemoval = true)
    @JsonManagedReference
    private PossessionAlternativesEntity possessionAlternativesEntity;

    @OneToOne(cascade = ALL, mappedBy = "claim", orphanRemoval = true)
    @JsonManagedReference
    private RentArrearsEntity rentArrears;

    @OneToOne(cascade = ALL, mappedBy = "claim", orphanRemoval = true)
    @JsonManagedReference
    private NoticeOfPossessionEntity noticeOfPossession;

    @OneToOne(cascade = ALL, mappedBy = "claim", orphanRemoval = true)
    @JsonManagedReference
    private StatementOfTruthEntity statementOfTruth;

    public void setHousingActWales(HousingActWalesEntity housingActWales) {
        if (this.housingActWales != null) {
            this.housingActWales.setClaim(null);
        }

        this.housingActWales = housingActWales;

        if (this.housingActWales != null) {
            this.housingActWales.setClaim(this);
        }
    }

    public void setAsbProhibitedConductEntity(AsbProhibitedConductEntity asbProhibitedConductEntity) {
        if (this.asbProhibitedConductEntity != null) {
            this.asbProhibitedConductEntity.setClaim(null);
        }

        this.asbProhibitedConductEntity = asbProhibitedConductEntity;

        if (this.asbProhibitedConductEntity != null) {
            this.asbProhibitedConductEntity.setClaim(this);
        }
    }

    public void setPossessionAlternativesEntity(PossessionAlternativesEntity possessionAlternativesEntity) {
        if (this.possessionAlternativesEntity != null) {
            this.possessionAlternativesEntity.setClaim(null);
        }

        this.possessionAlternativesEntity = possessionAlternativesEntity;

        if (this.possessionAlternativesEntity != null) {
            this.possessionAlternativesEntity.setClaim(this);
        }
    }

    public void setRentArrears(RentArrearsEntity rentArrears) {
        if (this.rentArrears != null) {
            this.rentArrears.setClaim(null);
        }

        this.rentArrears = rentArrears;

        if (this.rentArrears != null) {
            this.rentArrears.setClaim(this);
        }
    }

    public void setNoticeOfPossession(NoticeOfPossessionEntity noticeOfPossession) {
        if (this.noticeOfPossession != null) {
            this.noticeOfPossession.setClaim(null);
        }

        this.noticeOfPossession = noticeOfPossession;

        if (this.noticeOfPossession != null) {
            this.noticeOfPossession.setClaim(this);
        }
    }

    public void setStatementOfTruth(StatementOfTruthEntity statementOfTruth) {
        if (this.statementOfTruth != null) {
            this.statementOfTruth.setClaim(null);
        }

        this.statementOfTruth = statementOfTruth;

        if (this.statementOfTruth != null) {
            this.statementOfTruth.setClaim(this);
        }
    }

    public void addParty(PartyEntity party, PartyRole partyRole) {
        ClaimPartyEntity claimPartyEntity = ClaimPartyEntity.builder()
            .claim(this)
            .party(party)
            .role(partyRole)
            .build();
        claimParties.add(claimPartyEntity);
        party.getClaimParties().add(claimPartyEntity);
    }

    public void addClaimGrounds(List<ClaimGroundEntity> grounds) {
        for (ClaimGroundEntity ground : grounds) {
            ground.setClaim(this);
            this.claimGrounds.add(ground);
        }
    }

    public void addClaimDocuments(List<DocumentEntity> documents) {

        for (DocumentEntity document : documents) {
            ClaimDocumentEntity claimDocument = ClaimDocumentEntity.builder()
                .claim(this)
                .document(document)
                .build();

            claimDocuments.add(claimDocument);
            document.getClaimDocuments().add(claimDocument);
        }
    }
}
