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

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

/**
 * CCD domain complex type for rent arrears details.
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class RentArrearsSection {

    public static final String RECOVERY_ATTEMPT_DETAILS_LABEL =
        "Give details of previous steps taken to recover rent arrears";

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
        label = "Have there been previous steps taken to recover rent arrears?",
        hint = "This includes court proceedings"
    )
    private VerticalYesNo recoveryAttempted;

    @CCD(
        label = RECOVERY_ATTEMPT_DETAILS_LABEL,
        hint = "Include any case numbers if there were previous court proceedings. You can enter up to 500 characters.",
        typeOverride = TextArea
    )
    private String recoveryAttemptDetails;
}

