package uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.annotation.JacksonMoneyGBP;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import java.math.BigDecimal;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.MoneyGBP;

@Builder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CitizenClaimSubmission {

    private VerticalYesNo statementOfTruthAgreed;

    private String statementOfTruthFullName;

    private String statementOfTruthPosition;

    private String serviceRequestReference;

    @CCD(typeOverride = MoneyGBP)
    @JacksonMoneyGBP
    private BigDecimal feeAmount;

    private String paymentStatus;

}
