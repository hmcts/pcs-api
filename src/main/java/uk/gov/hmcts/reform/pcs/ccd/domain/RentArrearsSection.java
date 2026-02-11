package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CitizenAccess;
import uk.gov.hmcts.reform.pcs.ccd.annotation.JacksonMoneyGBP;

import java.math.BigDecimal;
import java.util.List;

/**
 * CCD domain complex type for rent arrears details.
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class RentArrearsSection {

    @CCD(
        label = "Add document",
        hint = "Upload a document to the system",
        typeOverride = FieldType.Collection,
        typeParameterOverride = "Document"
    )
    private List<ListValue<Document>> statementDocuments;

    @CCD(
        label = "Total rent arrears",
        min = 0,
        typeOverride = FieldType.MoneyGBP,
        access = {CitizenAccess.class}
    )
    @JacksonMoneyGBP
    private BigDecimal total;

    @CCD(
        label = "For the period shown on the rent statement, have any rent payments been paid by someone "
            + "other than the defendants?",
        hint = "This could include payments from Universal Credit, Housing Benefit or any other contributions "
            + "made by a government department, like the Department for Work and Pensions (DWP)"
    )
    private VerticalYesNo thirdPartyPayments;

    @CCD(
        label = "Where have the payments come from?",
        hint = "Select all that apply",
        typeOverride = FieldType.MultiSelectList,
        typeParameterOverride = "ThirdPartyPaymentSource"
    )
    private List<ThirdPartyPaymentSource> thirdPartyPaymentSources;

    @CCD(
        label = "Payment source",
        max = 60
    )
    private String paymentSourceOther;
}

