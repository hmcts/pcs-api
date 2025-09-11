package uk.gov.hmcts.reform.pcs.ccd.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Entity;
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
import uk.gov.hmcts.reform.pcs.ccd.domain.model.NoRentArrearsReasonForGrounds;

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
    private Set<ClaimPartyEntity> claimParties = new HashSet<>();

    @OneToMany(fetch = LAZY, cascade = ALL, mappedBy = "claim")
    @Builder.Default
    @JsonManagedReference
    private Set<ClaimGroundEntity> claimGroundEntities = new HashSet<>();

    private String summary;

    public void addParty(PartyEntity party, PartyRole partyRole) {
        ClaimPartyEntity claimPartyEntity = ClaimPartyEntity.builder()
            .claim(this)
            .party(party)
            .role(partyRole)
            .build();

        claimParties.add(claimPartyEntity);
        party.getClaimParties().add(claimPartyEntity);
    }

    public void addClaimGroundEntities(NoRentArrearsReasonForGrounds grounds) {
        List<ClaimGroundEntity> claimGroundEntityList = collectGroundEntities(grounds);
        claimGroundEntities.addAll(claimGroundEntityList);
    }

    private List<ClaimGroundEntity> collectGroundEntities(NoRentArrearsReasonForGrounds grounds) {
        List<ClaimGroundEntity> result = new ArrayList<>();

        if (grounds.getOwnerOccupierTextArea() != null) {
            result.add(ClaimGroundEntity.builder()
                           .claim(this)
                           .groundsId("Owner occupier (ground 1)")
                           .claimsReasonText(grounds.getOwnerOccupierTextArea())
                           .build());
        }
        if (grounds.getRepossessionByLenderTextArea() != null) {
            result.add(ClaimGroundEntity.builder()
                           .claim(this)
                           .groundsId("Repossession by the landlord's mortgage lender (ground 2)")
                           .claimsReasonText(grounds.getRepossessionByLenderTextArea())
                           .build());
        }
        if (grounds.getHolidayLetTextArea() != null) {
            result.add(ClaimGroundEntity.builder()
                           .claim(this)
                           .groundsId("Holiday let (ground 3)")
                           .claimsReasonText(grounds.getHolidayLetTextArea())
                           .build());
        }
        if (grounds.getStudentLetTextArea() != null) {
            result.add(ClaimGroundEntity.builder()
                           .claim(this)
                           .groundsId("Student let (ground 4)")
                           .claimsReasonText(grounds.getStudentLetTextArea())
                           .build());
        }
        if (grounds.getMinisterOfReligionTextArea() != null) {
            result.add(ClaimGroundEntity.builder()
                           .claim(this)
                           .groundsId("Property required for minister of religion (ground 5)")
                           .claimsReasonText(grounds.getMinisterOfReligionTextArea())
                           .build());
        }
        if (grounds.getRedevelopmentTextArea() != null) {
            result.add(ClaimGroundEntity.builder()
                           .claim(this)
                           .groundsId("Property required for redevelopment (ground 6)")
                           .claimsReasonText(grounds.getRedevelopmentTextArea())
                           .build());
        }
        if (grounds.getDeathOfTenantTextArea() != null) {
            result.add(ClaimGroundEntity.builder()
                           .claim(this)
                           .groundsId("Death of the tenant (ground 7)")
                           .claimsReasonText(grounds.getDeathOfTenantTextArea())
                           .build());
        }
        if (grounds.getAntisocialBehaviourTextArea() != null) {
            result.add(ClaimGroundEntity.builder()
                           .claim(this)
                           .groundsId("Antisocial behaviour (ground 7A)")
                           .claimsReasonText(grounds.getAntisocialBehaviourTextArea())
                           .build());
        }
        if (grounds.getNoRightToRentTextArea() != null) {
            result.add(ClaimGroundEntity.builder()
                           .claim(this)
                           .groundsId("Tenant does not have a right to rent (ground 7B)")
                           .claimsReasonText(grounds.getNoRightToRentTextArea())
                           .build());
        }
        if (grounds.getSeriousRentArrearsTextArea() != null) {
            result.add(ClaimGroundEntity.builder()
                           .claim(this)
                           .groundsId("Serious rent arrears (ground 8)")
                           .claimsReasonText(grounds.getSeriousRentArrearsTextArea())
                           .build());
        }
        if (grounds.getSuitableAlternativeAccommodationTextArea() != null) {
            result.add(ClaimGroundEntity.builder()
                           .claim(this)
                           .groundsId("Suitable alternative accommodation (ground 9)")
                           .claimsReasonText(grounds.getSuitableAlternativeAccommodationTextArea())
                           .build());
        }
        if (grounds.getRentArrearsTextArea() != null) {
            result.add(ClaimGroundEntity.builder()
                           .claim(this)
                           .groundsId("Rent arrears (ground 10)")
                           .claimsReasonText(grounds.getRentArrearsTextArea())
                           .build());
        }
        if (grounds.getPersistentDelayInPayingRentTextArea() != null) {
            result.add(ClaimGroundEntity.builder()
                           .claim(this)
                           .groundsId("Persistent delay in paying rent (ground 11)")
                           .claimsReasonText(grounds.getPersistentDelayInPayingRentTextArea())
                           .build());
        }
        if (grounds.getBreachOfTenancyConditionsTextArea() != null) {
            result.add(ClaimGroundEntity.builder()
                           .claim(this)
                           .groundsId("Breach of tenancy conditions (ground 12)")
                           .claimsReasonText(grounds.getBreachOfTenancyConditionsTextArea())
                           .build());
        }
        if (grounds.getPropertyDeteriorationTextArea() != null) {
            result.add(ClaimGroundEntity.builder()
                           .claim(this)
                           .groundsId("Deterioration in the condition of the property (ground 13)")
                           .claimsReasonText(grounds.getPropertyDeteriorationTextArea())
                           .build());
        }
        if (grounds.getNuisanceOrIllegalUseTextArea() != null) {
            result.add(ClaimGroundEntity.builder()
                           .claim(this)
                           .groundsId("Nuisance, annoyance, illegal or immoral use of the property (ground 14)")
                           .claimsReasonText(grounds.getNuisanceOrIllegalUseTextArea())
                           .build());
        }
        if (grounds.getDomesticViolenceTextArea() != null) {
            result.add(ClaimGroundEntity.builder()
                           .claim(this)
                           .groundsId("Domestic violence (ground 14A)")
                           .claimsReasonText(grounds.getDomesticViolenceTextArea())
                           .build());
        }
        if (grounds.getOffenceDuringRiotTextArea() != null) {
            result.add(ClaimGroundEntity.builder()
                           .claim(this)
                           .groundsId("Offence during a riot (ground 14ZA)")
                           .claimsReasonText(grounds.getOffenceDuringRiotTextArea())
                           .build());
        }
        if (grounds.getFurnitureDeteriorationTextArea() != null) {
            result.add(ClaimGroundEntity.builder()
                           .claim(this)
                           .groundsId("Deterioration of furniture (ground 15)")
                           .claimsReasonText(grounds.getFurnitureDeteriorationTextArea())
                           .build());
        }
        if (grounds.getLandlordEmployeeTextArea() != null) {
            result.add(ClaimGroundEntity.builder()
                           .claim(this)
                           .groundsId("Employee of the landlord (ground 16)")
                           .claimsReasonText(grounds.getLandlordEmployeeTextArea())
                           .build());
        }
        if (grounds.getFalseStatementTextArea() != null) {
            result.add(ClaimGroundEntity.builder()
                           .claim(this)
                           .groundsId("Tenancy obtained by false statement (ground 17)")
                           .claimsReasonText(grounds.getFalseStatementTextArea())
                           .build());
        }
        return result;
    }
}
