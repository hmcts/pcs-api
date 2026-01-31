package uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.annotation.JacksonMoneyGBP;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimantProvidedInfo {

    @CCD
    private Party party;

    @CCD
    private String claimantOrg;

    @CCD
    private LegislativeCountry legislativeCountry;

    @CCD
    private String tenancyType;

    @CCD
    private LocalDate tenancyStartDate;

    @CCD(typeOverride = FieldType.MoneyGBP)
    @JacksonMoneyGBP
    private BigDecimal dailyRentAmount;

    @CCD(typeOverride = FieldType.MoneyGBP)
    @JacksonMoneyGBP
    private BigDecimal rentArrearsOwed;

    @CCD
    private YesOrNo noticeServed;

    @CCD
    private LocalDateTime noticeDate;
}
