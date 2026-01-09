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
import uk.gov.hmcts.reform.pcs.ccd.domain.DemotionOfTenancyHousingAct;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuyHousingAct;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.ASBQuestionsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.ProhibitedConductWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.EnforcementOrderEntity;
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

    @OneToMany(fetch = LAZY, cascade = ALL, mappedBy = "claim")
    @Builder.Default
    @JsonManagedReference
    private List<ClaimPartyEntity> claimParties = new ArrayList<>();

    @OneToMany(fetch = LAZY, cascade = ALL, mappedBy = "claim")
    @Builder.Default
    @JsonManagedReference
    private Set<ClaimGroundEntity> claimGrounds = new HashSet<>();

    private String summary;

    private Boolean applicationWithClaim;

    private String defendantCircumstances;

    private Boolean costsClaimed;

    @Enumerated(EnumType.STRING)
    private SuspensionOfRightToBuyHousingAct suspensionOfRightToBuyHousingAct;

    private String suspensionOfRightToBuyReason;

    @Enumerated(EnumType.STRING)
    private DemotionOfTenancyHousingAct demotionOfTenancyHousingAct;

    private String demotionOfTenancyReason;

    private String statementOfExpressTermsDetails;

    private String additionalReasons;

    private String claimantCircumstances;

    @Enumerated(EnumType.STRING)
    private LanguageUsed languageUsed;

    @JdbcTypeCode(SqlTypes.JSON)
    private ProhibitedConductWales prohibitedConduct;

    @JdbcTypeCode(SqlTypes.JSON)
    private ASBQuestionsWales asbQuestions;

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
