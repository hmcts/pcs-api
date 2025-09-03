package uk.gov.hmcts.reform.pcs.ccd.domain;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerAccess;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SecureOrFlexibleGroundsReasons {

   //DiscretionaryGrounds
   @CCD(
       label = """
        ---
        <h2>Breach of the tenancy (ground 1)</h2>
        """,
       typeOverride = FieldType.Label
   )
   private String breachOfTenancyLabel;

    @CCD(
        label = "Why are you making a claim for possession under this ground?",
        hint = "Give details about your reason for possession",
        typeOverride = TextArea,
        access = { CaseworkerAccess.class }
    )
    private String breachOfTenancyGround;

    @CCD(
        label = """
        ---
        <h2>Nuisance, annoyance, illegal or immoral use of the property (ground 2)</h2>
        """,
        typeOverride = FieldType.Label
    )
    private String nuisanceOrImmoralUseLabel;

    @CCD(
        label = "Why are you making a claim for possession under this ground?",
        hint = "Give details about your reason for possession",
        typeOverride = TextArea,
        access = { CaseworkerAccess.class }
    )
    private String nuisanceOrImmoralUseGround;

    @CCD(
        label = """
        ---
        <h2>Domestic violence (ground 2A)</h2>
        """,
        typeOverride = FieldType.Label
    )
    private String domesticViolenceLabel;

    @CCD(
        label = "Why are you making a claim for possession under this ground?",
        hint = "Give details about your reason for possession",
        typeOverride = TextArea,
        access = { CaseworkerAccess.class }
    )
    private String domesticViolenceGround;

    @CCD(
        label = """
        ---
        <h2>Offence during a riot (ground 22A)</h2>
        """,
        typeOverride = FieldType.Label
    )
    private String riotOffenceLabel;

    @CCD(
        label = "Why are you making a claim for possession under this ground?",
        hint = "Give details about your reason for possession",
        typeOverride = TextArea,
        access = { CaseworkerAccess.class }
    )
    private String riotOffenceGround;

    @CCD(
        label = """
        ---
        <h2>Deterioration in the condition of the property (ground 3)</h2>
        """,
        typeOverride = FieldType.Label
    )
    private String propertyDeteriorationLabel;

    @CCD(
        label = "Why are you making a claim for possession under this ground?",
        hint = "Give details about your reason for possession",
        typeOverride = TextArea,
        access = { CaseworkerAccess.class }
    )
    private String propertyDeteriorationGround;

    @CCD(
        label = """
        ---
        <h2>Deterioration of furniture (ground 4)</h2>
        """,
        typeOverride = FieldType.Label
    )
    private String furnitureDeteriorationLabel;

    @CCD(
        label = "Why are you making a claim for possession under this ground?",
        hint = "Give details about your reason for possession",
        typeOverride = TextArea,
        access = { CaseworkerAccess.class }
    )
    private String furnitureDeteriorationGround;

    @CCD(
        label = """
        ---
        <h2>Tenancy obtained by false statement (ground 5)</h2>
        """,
        typeOverride = FieldType.Label
    )
    private String tenancyObtainedByFalseStatementLabel;

    @CCD(
        label = "Why are you making a claim for possession under this ground?",
        hint = "Give details about your reason for possession",
        typeOverride = TextArea,
        access = { CaseworkerAccess.class }
    )
    private String tenancyObtainedByFalseStatementGround;

    @CCD(
        label = """
        ---
        <h2>Premium paid in connection with mutual exchange (ground 6)</h2>
        """,
        typeOverride = FieldType.Label
    )
    private String premiumPaidMutualExchangeLabel;

    @CCD(
        label = "Why are you making a claim for possession under this ground?",
        hint = "Give details about your reason for possession",
        typeOverride = TextArea,
        access = { CaseworkerAccess.class }
    )
    private String premiumPaidMutualExchangeGround;

    @CCD(
        label = """
        ---
        <h2>Unreasonable conduct in tied accommodation (ground 7)</h2>
        """,
        typeOverride = FieldType.Label
    )
    private String unreasonableConductTiedAccommodationLabel;

    @CCD(
        label = "Why are you making a claim for possession under this ground?",
        hint = "Give details about your reason for possession",
        typeOverride = TextArea,
        access = { CaseworkerAccess.class }
    )
    private String unreasonableConductTiedAccommodationGround;

    @CCD(
        label = """
        ---
        <h2>Refusal to move back to main home after works completed (ground 8)</h2>
        """,
        typeOverride = FieldType.Label
    )
    private String refusalToMoveBackLabel;

    @CCD(
        label = "Why are you making a claim for possession under this ground?",
        hint = "Give details about your reason for possession",
        typeOverride = TextArea,
        access = { CaseworkerAccess.class }
    )
    private String refusalToMoveBackGround;

// --------- Discretionary Grounds (alt. accommodation) ----------
    @CCD(
        label = """
        ---
        <h2>Tied accommodation needed for another employee (ground 12)</h2>
        """,
        typeOverride = FieldType.Label
    )
    private String tiedAccommodationNeededForEmployeeLabel;

    @CCD(
        label = "Why are you making a claim for possession under this ground?",
        hint = "Give details about your reason for possession",
        typeOverride = TextArea,
        access = { CaseworkerAccess.class }
    )
    private String tiedAccommodationNeededForEmployeeGround;

    @CCD(
        label = """
        ---
        <h2>Adapted accommodation (ground 13)</h2>
        """,
        typeOverride = FieldType.Label
    )
    private String adaptedAccommodationLabel;

    @CCD(
        label = "Why are you making a claim for possession under this ground?",
        hint = "Give details about your reason for possession",
        typeOverride = TextArea,
        access = { CaseworkerAccess.class }
    )
    private String adaptedAccommodationGround;

    @CCD(
        label = """
        ---
        <h2>Housing association special circumstances accommodation (ground 14)</h2>
        """,
        typeOverride = FieldType.Label
    )
    private String housingAssociationSpecialCircumstancesLabel;

    @CCD(
        label = "Why are you making a claim for possession under this ground?",
        hint = "Give details about your reason for possession",
        typeOverride = TextArea,
        access = { CaseworkerAccess.class }
    )
    private String housingAssociationSpecialCircumstancesGround;

    @CCD(
        label = """
        ---
        <h2>Special needs accommodation (ground 15)</h2>
        """,
        typeOverride = FieldType.Label
    )
    private String specialNeedsAccommodationLabel;

    @CCD(
        label = "Why are you making a claim for possession under this ground?",
        hint = "Give details about your reason for possession",
        typeOverride = TextArea,
        access = { CaseworkerAccess.class }
    )
    private String specialNeedsAccommodationGround;

    @CCD(
        label = """
        ---
        <h2>Under occupying after succession (ground 15A)</h2>
        """,
        typeOverride = FieldType.Label
    )
    private String underOccupyingAfterSuccessionLabel;

    @CCD(
        label = "Why are you making a claim for possession under this ground?",
        hint = "Give details about your reason for possession",
        typeOverride = TextArea,
        access = { CaseworkerAccess.class }
    )
    private String underOccupyingAfterSuccessionGround;

    //Mandatory grounds
    @CCD(
        label = """
        ---
        <h2>Antisocial behaviour</h2>
        """,
        typeOverride = FieldType.Label
    )
    private String antiSocialLabel;

    @CCD(
        label = "Why are you making a claim for possession under this ground?",
        hint = "Give details about your reason for possession",
        typeOverride = TextArea,
        access = { CaseworkerAccess.class }
    )
    private String antiSocialGround;

    @CCD(
        label = """
        ---
        <h2>Overcrowding (ground 9)</h2>
        """,
        typeOverride = FieldType.Label
    )
    private String overcrowdingLabel;

    @CCD(
        label = "Why are you making a claim for possession under this ground?",
        hint = "Give details about your reason for possession",
        typeOverride = TextArea,
        access = { CaseworkerAccess.class }
    )
    private String overcrowdingGround;

    @CCD(
        label = """
        ---
        <h2>Landlord's works (ground 10)</h2>
        """,
        typeOverride = FieldType.Label
    )
    private String landlordWorksLabel;

    @CCD(
        label = "Why are you making a claim for possession under this ground?",
        hint = "Give details about your reason for possession",
        typeOverride = TextArea,
        access = { CaseworkerAccess.class }
    )
    private String landlordWorksGround;

    @CCD(
        label = """
        ---
        <h2>Property sold for redevelopment (ground 10A)</h2>
        """,
        typeOverride = FieldType.Label
    )
    private String propertySoldLabel;

    @CCD(
        label = "Why are you making a claim for possession under this ground?",
        hint = "Give details about your reason for possession",
        typeOverride = TextArea,
        access = { CaseworkerAccess.class }
    )
    private String propertySoldGround;

    @CCD(
        label = """
        ---
        <h2>Charitable landlords (ground 11)</h2>
        """,
        typeOverride = FieldType.Label
    )
    private String charitableLandlordLabel;

    @CCD(
        label = "Why are you making a claim for possession under this ground?",
        hint = "Give details about your reason for possession",
        typeOverride = TextArea,
        access = { CaseworkerAccess.class }
    )
    private String charitableLandlordGround;

}
