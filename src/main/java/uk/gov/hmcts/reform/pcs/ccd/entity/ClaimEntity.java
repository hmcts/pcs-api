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
import uk.gov.hmcts.reform.pcs.ccd.domain.DemotionOfTenancyHousingAct;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuyHousingAct;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.ASBQuestionsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.ProhibitedConductWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcement.EnforcementOrderEntity;

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

    // Columns to remove later when possession_alternatives table is implemented

    @Enumerated(EnumType.STRING)
    private SuspensionOfRightToBuyHousingAct suspensionOfRightToBuyHousingAct;

    private String suspensionOfRightToBuyReason;

    @Enumerated(EnumType.STRING)
    private DemotionOfTenancyHousingAct demotionOfTenancyHousingAct;

    private String demotionOfTenancyReason;

    private String statementOfExpressTermsDetails;

    @JdbcTypeCode(SqlTypes.JSON)
    private ProhibitedConductWales prohibitedConduct;

    @JdbcTypeCode(SqlTypes.JSON)
    private ASBQuestionsWales asbQuestions;

    @OneToMany(fetch = LAZY, cascade = ALL, mappedBy = "claim")
    @Builder.Default
    @JsonManagedReference
    private Set<ClaimPartyEntity> claimParties = new HashSet<>();

    @OneToMany(fetch = LAZY, cascade = ALL, mappedBy = "claim")
    @Builder.Default
    @JsonManagedReference
    private Set<ClaimGroundEntity> claimGrounds = new HashSet<>();

    @OneToMany(fetch = LAZY, cascade = ALL, mappedBy = "claim")
    @Builder.Default
    @JsonManagedReference
    private Set<EnforcementOrderEntity> enforcementOrders = new HashSet<>();

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
}
