package uk.gov.hmcts.reform.pcs.ccd.domain.caseworker;

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
import uk.gov.hmcts.reform.pcs.ccd.annotation.JacksonMoneyGBP;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase.MAX_MONETARY_AMOUNT;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class EnterGenAppRequest {

    @CCD(label = "What date was the application received?", hint = "For example, 16 4 2021")
    private LocalDate dateReceived;

    @CCD(label = "Which type of application has the applicant made?")
    private EnterGenAppType applicationTypeOption;

    @CCD(
        label = "Which categories apply?",
        typeOverride = FieldType.TextArea
    )
    private String somethingElseDetails;

    @CCD(label = "Is there a hearing for this case in the next 14 days?")
    private VerticalYesNo within14Days;

    @CCD(label = "Has HMCTS received the application fee?")
    private VerticalYesNo feeReceived;

    @CCD(label = "Enter the amount received",
        typeOverride = FieldType.MoneyGBP,
        max = MAX_MONETARY_AMOUNT
    )
    @JacksonMoneyGBP
    private BigDecimal feeAmountReceived;

    @CCD(label = "Has the applicant included a Help With Fees reference number on their application?")
    private VerticalYesNo appliedForHwf;

    @CCD(
        label = "Enter their Help with Fees reference number",
        max = 60
    )
    private String hwfReference;

    @CCD(
        label = "Add document",
        hint = "Upload a document to the system",
        searchable = false
    )
    private List<ListValue<Document>> relatedEvidence;

}
