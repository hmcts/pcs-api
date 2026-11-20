package uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.annotation.JacksonMoneyGBP;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.MoneyGBP;
import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Builder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CitizenClaimRentArrears {

    @CCD(typeOverride = MoneyGBP)
    @JacksonMoneyGBP
    private BigDecimal rentAmount;

    private RentPaymentFrequency rentFrequency;

    private String otherFrequencyDetails;

    private Integer rentDueDay;

    @CCD(typeOverride = MoneyGBP)
    @JacksonMoneyGBP
    private BigDecimal arrearsAtNoticeDate;

    @CCD(typeOverride = MoneyGBP)
    @JacksonMoneyGBP
    private BigDecimal currentArrears;

    private LocalDate currentArrearsDate;

    private CitizenClaimRentProofType proofType;

    private List<ListValue<Document>> rentStatementDocuments;

    private List<ListValue<CitizenClaimRentLedgerEntry>> rentHistoryLedger;

    private List<ListValue<CitizenClaimRentPeriodEntry>> rentHistoryTable;

    private VerticalYesNo moneyJudgmentWanted;

    @CCD(typeOverride = TextArea)
    private String recoverySteps;

}
