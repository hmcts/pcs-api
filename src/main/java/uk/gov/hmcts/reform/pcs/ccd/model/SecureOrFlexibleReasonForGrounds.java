package uk.gov.hmcts.reform.pcs.ccd.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SecureOrFlexibleReasonForGrounds {
    private String breachOfTenancyGround;
    private String nuisanceOrImmoralUseGround;
    private String domesticViolenceGround;
    private String riotOffenceGround;
    private String propertyDeteriorationGround;
    private String furnitureDeteriorationGround;
    private String tenancyObtainedByFalseStatementGround;
    private String premiumPaidMutualExchangeGround;
    private String unreasonableConductTiedAccommodationGround;
    private String refusalToMoveBackGround;
    private String tiedAccommodationNeededForEmployeeGround;
    private String adaptedAccommodationGround;
    private String housingAssociationSpecialCircumstancesGround;
    private String specialNeedsAccommodationGround;
    private String underOccupyingAfterSuccessionGround;
    private String antiSocialGround;
    private String overcrowdingGround;
    private String landlordWorksGround;
    private String propertySoldGround;
    private String charitableLandlordGround;
}
