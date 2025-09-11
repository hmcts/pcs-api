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
import uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.model.NoRentArrearsReasonForGrounds;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

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

    private final Map<NoRentArrearsMandatoryGrounds,
        Function<NoRentArrearsReasonForGrounds, String>> NO_RENT_ARREARS_MANDATORY_ACCESSORS =
        Map.ofEntries(
            Map.entry(NoRentArrearsMandatoryGrounds.OWNER_OCCUPIER,
                      NoRentArrearsReasonForGrounds::getOwnerOccupierTextArea),
            Map.entry(NoRentArrearsMandatoryGrounds.REPOSSESSION_BY_LENDER,
                      NoRentArrearsReasonForGrounds::getRepossessionByLenderTextArea),
            Map.entry(NoRentArrearsMandatoryGrounds.HOLIDAY_LET,
                      NoRentArrearsReasonForGrounds::getHolidayLetTextArea),
            Map.entry(NoRentArrearsMandatoryGrounds.STUDENT_LET,
                      NoRentArrearsReasonForGrounds::getStudentLetTextArea),
            Map.entry(NoRentArrearsMandatoryGrounds.MINISTER_OF_RELIGION,
                      NoRentArrearsReasonForGrounds::getMinisterOfReligionTextArea),
            Map.entry(NoRentArrearsMandatoryGrounds.REDEVELOPMENT,
                      NoRentArrearsReasonForGrounds::getRedevelopmentTextArea),
            Map.entry(NoRentArrearsMandatoryGrounds.DEATH_OF_TENANT,
                      NoRentArrearsReasonForGrounds::getDeathOfTenantTextArea),
            Map.entry(NoRentArrearsMandatoryGrounds.ANTISOCIAL_BEHAVIOUR,
                      NoRentArrearsReasonForGrounds::getAntisocialBehaviourTextArea),
            Map.entry(NoRentArrearsMandatoryGrounds.NO_RIGHT_TO_RENT,
                      NoRentArrearsReasonForGrounds::getNoRightToRentTextArea),
            Map.entry(NoRentArrearsMandatoryGrounds.SERIOUS_RENT_ARREARS,
                      NoRentArrearsReasonForGrounds::getSeriousRentArrearsTextArea)
        );

    private final Map<NoRentArrearsDiscretionaryGrounds,
        Function<NoRentArrearsReasonForGrounds, String>> NO_RENT_ARREARS_DISCRETIONARY_ACCESSORS =
        Map.ofEntries(
            Map.entry(NoRentArrearsDiscretionaryGrounds.SUITABLE_ALTERNATIVE_ACCOMMODATION,
                      NoRentArrearsReasonForGrounds::getSuitableAlternativeAccommodationTextArea),
            Map.entry(NoRentArrearsDiscretionaryGrounds.RENT_ARREARS,
                      NoRentArrearsReasonForGrounds::getRentArrearsTextArea),
            Map.entry(NoRentArrearsDiscretionaryGrounds.PERSISTENT_DELAY_IN_PAYING_RENT,
                      NoRentArrearsReasonForGrounds::getPersistentDelayInPayingRentTextArea),
            Map.entry(NoRentArrearsDiscretionaryGrounds.BREACH_OF_TENANCY_CONDITIONS,
                      NoRentArrearsReasonForGrounds::getBreachOfTenancyConditionsTextArea),
            Map.entry(NoRentArrearsDiscretionaryGrounds.PROPERTY_DETERIORATION,
                      NoRentArrearsReasonForGrounds::getPropertyDeteriorationTextArea),
            Map.entry(NoRentArrearsDiscretionaryGrounds.NUISANCE_OR_ILLEGAL_USE,
                      NoRentArrearsReasonForGrounds::getNuisanceOrIllegalUseTextArea),
            Map.entry(NoRentArrearsDiscretionaryGrounds.DOMESTIC_VIOLENCE,
                      NoRentArrearsReasonForGrounds::getDomesticViolenceTextArea),
            Map.entry(NoRentArrearsDiscretionaryGrounds.OFFENCE_DURING_RIOT,
                      NoRentArrearsReasonForGrounds::getOffenceDuringRiotTextArea),
            Map.entry(NoRentArrearsDiscretionaryGrounds.FURNITURE_DETERIORATION,
                      NoRentArrearsReasonForGrounds::getFurnitureDeteriorationTextArea),
            Map.entry(NoRentArrearsDiscretionaryGrounds.LANDLORD_EMPLOYEE,
                      NoRentArrearsReasonForGrounds::getLandlordEmployeeTextArea),
            Map.entry(NoRentArrearsDiscretionaryGrounds.FALSE_STATEMENT,
                      NoRentArrearsReasonForGrounds::getFalseStatementTextArea)
        );

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
        List<ClaimGroundEntity> entities = new ArrayList<>();

        // NoRentArrearsMandatoryGrounds
        for (Map.Entry<NoRentArrearsMandatoryGrounds, Function<NoRentArrearsReasonForGrounds, String>> entry :
            NO_RENT_ARREARS_MANDATORY_ACCESSORS.entrySet()) {
            String value = entry.getValue().apply(grounds);
            if (value != null) {
                entities.add(ClaimGroundEntity.builder()
                                 .claim(this)
                                 .groundsId(entry.getKey().name())
                                 .claimsReasonText(value)
                                 .build());
            }
        }

        // NoRentArrearsDiscretionaryGrounds
        for (Map.Entry<NoRentArrearsDiscretionaryGrounds, Function<NoRentArrearsReasonForGrounds, String>> entry :
            NO_RENT_ARREARS_DISCRETIONARY_ACCESSORS.entrySet()) {
            String value = entry.getValue().apply(grounds);
            if (value != null) {
                entities.add(ClaimGroundEntity.builder()
                                 .claim(this)
                                 .groundsId(entry.getKey().name())
                                 .claimsReasonText(value)
                                 .build());
            }
        }
        claimGroundEntities.addAll(entities);
    }
}
