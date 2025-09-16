package uk.gov.hmcts.reform.pcs.ccd.domain;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerAccess;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SecureOrFlexibleGroundsReasons {

    @CCD(
        label = "Give details about your reason for possession",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        max = 500,
        access = { CaseworkerAccess.class }
    )
    private String breachOfTenancyGround;

    @CCD(
        label = "Give details about your reason for possession",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        max = 500,
        access = { CaseworkerAccess.class }
    )
    private String nuisanceOrImmoralUseGround;

    @CCD(
        label = "Give details about your reason for possession",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        max = 500,
        access = { CaseworkerAccess.class }
    )
    private String domesticViolenceGround;

    @CCD(
        label = "Give details about your reason for possession",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        max = 500,
        access = { CaseworkerAccess.class }
    )
    private String riotOffenceGround;

    @CCD(
        label = "Give details about your reason for possession",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        max = 500,
        access = { CaseworkerAccess.class }
    )
    private String propertyDeteriorationGround;

    @CCD(
        label = "Give details about your reason for possession",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        max = 500,
        access = { CaseworkerAccess.class }
    )
    private String furnitureDeteriorationGround;

    @CCD(
        label = "Give details about your reason for possession",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        max = 500,
        access = { CaseworkerAccess.class }
    )
    private String tenancyByFalseStatementGround;

    @CCD(
        label = "Give details about your reason for possession",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        max = 500,
        access = { CaseworkerAccess.class }
    )
    private String premiumMutualExchangeGround;

    @CCD(
        label = "Give details about your reason for possession",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        max = 500,
        access = { CaseworkerAccess.class }
    )
    private String unreasonableConductGround;

    @CCD(
        label = "Give details about your reason for possession",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        max = 500,
        access = { CaseworkerAccess.class }
    )
    private String refusalToMoveBackGround;

    @CCD(
        label = "Give details about your reason for possession",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        max = 500,
        access = { CaseworkerAccess.class }
    )
    private String tiedAccommodationGround;

    @CCD(
        label = "Give details about your reason for possession",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        max = 500,
        access = { CaseworkerAccess.class }
    )
    private String adaptedAccommodationGround;

    @CCD(
        label = "Give details about your reason for possession",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        max = 500,
        access = { CaseworkerAccess.class }
    )
    private String housingAssocSpecialGround;

    @CCD(
        label = "Give details about your reason for possession",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        max = 500,
        access = { CaseworkerAccess.class }
    )
    private String specialNeedsAccommodationGround;

    @CCD(
        label = "Give details about your reason for possession",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        max = 500,
        access = { CaseworkerAccess.class }
    )
    private String underOccupancySuccessionGround;

    @CCD(
        label = "Give details about your reason for possession",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        max = 500,
        access = { CaseworkerAccess.class }
    )
    private String antiSocialGround;

    @CCD(
        label = "Give details about your reason for possession",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        max = 500,
        access = { CaseworkerAccess.class }
    )
    private String overcrowdingGround;

    @CCD(
        label = "Give details about your reason for possession",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        max = 500,
        access = { CaseworkerAccess.class }
    )
    private String landlordWorksGround;

    @CCD(
        label = "Give details about your reason for possession",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        max = 500,
        access = { CaseworkerAccess.class }
    )
    private String propertySoldGround;

    @CCD(
        label = "Give details about your reason for possession",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        max = 500,
        access = { CaseworkerAccess.class }
    )
    private String charitableLandlordGround;

}
