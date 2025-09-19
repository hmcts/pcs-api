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
public class SecureOrFlexibleReasonsForGrounds {
    private String breachOfTenancyGround;
    private String nuisanceOrImmoralUseGround;
    private String domesticViolenceGround;
    private String riotOffenceGround;
    private String propertyDeteriorationGround;
    private String furnitureDeteriorationGround;
    private String tenancyByFalseStatementGround;
    private String premiumMutualExchangeGround;
    private String unreasonableConductGround;
    private String refusalToMoveBackGround;
    private String tiedAccommodationGround;
    private String adaptedAccommodationGround;
    private String housingAssocSpecialGround;
    private String specialNeedsAccommodationGround;
    private String underOccupancySuccessionGround;
    private String antiSocialGround;
    private String overcrowdingGround;
    private String landlordWorksGround;
    private String propertySoldGround;
    private String charitableLandlordGround;
}
