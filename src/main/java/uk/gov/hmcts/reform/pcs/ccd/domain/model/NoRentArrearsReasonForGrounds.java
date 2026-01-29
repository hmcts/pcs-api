package uk.gov.hmcts.reform.pcs.ccd.domain.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class NoRentArrearsReasonForGrounds {

    // Ground 1
    @CCD(
            label = "Give details about your reasons for claiming possession (Owner occupier (ground 1))",
            hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
                + "You can enter up to 500 characters",
            typeOverride = TextArea
    )
    private String ownerOccupier;

    // Ground 2
    @CCD(
            label = "Give details about your reasons for claiming possession (Repossession by the landlord’s"
                + " mortgage lender (ground 2))",
            hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
                + "You can enter up to 500 characters",
            typeOverride = TextArea
    )
    private String repossessionByLender;

    // Ground 3
    @CCD(
        label = "Give details about your reasons for claiming possession (Holiday let (ground 3))",
            hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
                + "You can enter up to 500 characters",
            typeOverride = TextArea
    )
    private String holidayLet;

    // Ground 4
    @CCD(
            label = "Give details about your reasons for claiming possession (Student let (ground 4))",
            hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
                + "You can enter up to 500 characters",
            typeOverride = TextArea
    )
    private String studentLet;

    // Ground 5
    @CCD(
            label = "Give details about your reasons for claiming possession (Property required for minister of"
            + " religion (ground 5))",
            hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
                + "You can enter up to 500 characters",
            typeOverride = TextArea
    )
    private String ministerOfReligion;

    // Ground 6
    @CCD(
            label = "Give details about your reasons for claiming possession (Property required for "
                + "redevelopment (ground 6))",
            hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
                + "You can enter up to 500 characters",
            typeOverride = TextArea
    )
    private String redevelopment;

    // Ground 7
    @CCD(
            label = "Give details about your reasons for claiming possession (Death of the tenant (ground 7))",
            hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
                + "You can enter up to 500 characters",
            typeOverride = TextArea
    )
    private String deathOfTenant;

    // Ground 7A
    @CCD(
            label = "Give details about your reasons for claiming possession (Antisocial behaviour (ground 7A))",
            hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
                + "You can enter up to 500 characters",
            typeOverride = TextArea
    )
    private String antisocialBehaviour;

    // Ground 7B
    @CCD(
            label = "Give details about your reasons for claiming possession (Tenant does not have a right "
                + "to rent (ground 7B))",
            hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
                + "You can enter up to 500 characters",
            typeOverride = TextArea
    )
    private String noRightToRent;

    // Ground 9
    @CCD(
            label = "Give details about your reasons for claiming possession (Suitable alternative"
                + " accommodation (ground 9))",
            hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
                + "You can enter up to 500 characters",
            typeOverride = TextArea
    )
    private String suitableAlternativeAccomodation;

    // Ground 12
    @CCD(
            label = "Give details about your reasons for claiming possession "
                + "(Breach of tenancy conditions (ground 12))",
            hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
                + "You can enter up to 500 characters",
            typeOverride = TextArea
    )
    private String breachOfTenancyConditions;

    // Ground 13
    @CCD(
            label = "Give details about your reasons for claiming possession (Deterioration in the condition "
                + "of the property (ground 13))",
            hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
                + "You can enter up to 500 characters",
            typeOverride = TextArea
    )
    private String propertyDeterioration;

    // Ground 14
    @CCD(
            label = "Give details about your reasons for claiming possession (Nuisance, annoyance, illegal or "
                + "immoral use of the property (ground 14))",
            hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
                + "You can enter up to 500 characters",
            typeOverride = TextArea
    )
    private String nuisanceOrIllegalUse;

    // Ground 14A
    @CCD(
            label = "Give details about your reasons for claiming possession (Domestic violence (ground 14A))",
            hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
                + "You can enter up to 500 characters",
            typeOverride = TextArea
    )
    private String domesticViolence;

    // Ground 14ZA
    @CCD(
            label = "Give details about your reasons for claiming possession (Offence during a riot (ground 14ZA))",
            hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
                + "You can enter up to 500 characters",
            typeOverride = TextArea
    )
    private String offenceDuringRiot;

    // Ground 15
    @CCD(
            label = "Give details about your reasons for claiming possession (Deterioration of furniture (ground 15))",
            hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
                + "You can enter up to 500 characters",
            typeOverride = TextArea
    )
    private String furnitureDeterioration;

    // Ground 16
    @CCD(
            label = "Give details about your reasons for claiming possession (Employee of the landlord (ground 16))",
            hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
                + "You can enter up to 500 characters",
            typeOverride = TextArea
    )
    private String landlordEmployee;

    // Ground 17
    @CCD(
            label = "Give details about your reasons for claiming possession (Tenancy obtained by false "
                + "statement (ground 17))",
            hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
                + "You can enter up to 500 characters",
            typeOverride = TextArea
    )
    private String falseStatement;
}
