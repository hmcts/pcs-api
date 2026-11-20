package uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.annotation.JacksonMoneyGBP;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.MoneyGBP;
import static uk.gov.hmcts.ccd.sdk.type.FieldType.MultiSelectList;

@Builder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CitizenClaimRentLedgerEntry {

    private LocalDate transactionDate;

    @CCD(typeOverride = MultiSelectList, typeParameterOverride = "CitizenClaimRentTransactionType")
    private Set<CitizenClaimRentTransactionType> transactionTypes;

    private String description;

    @CCD(typeOverride = MoneyGBP)
    @JacksonMoneyGBP
    private BigDecimal amountCharged;

    @CCD(typeOverride = MoneyGBP)
    @JacksonMoneyGBP
    private BigDecimal amountPaid;

}
