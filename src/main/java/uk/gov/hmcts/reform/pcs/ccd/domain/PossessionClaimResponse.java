package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PossessionClaimResponse {
    @CCD
    private YesOrNo contactByPhone;

    @CCD
    private Party party;

    // Claimant-provided fields (read-only, populated from database)
    @CCD
    private LegislativeCountry claimantProvidedLegislativeCountry;

    @CCD
    private String claimantProvidedTenancyType;

    @CCD
    private LocalDate claimantProvidedTenancyStartDate;

    @CCD
    private BigDecimal claimantProvidedDailyRentAmount;

    @CCD
    private BigDecimal claimantProvidedRentArrearsOwed;

    @CCD
    private YesOrNo claimantProvidedNoticeServed;

    @CCD
    private LocalDateTime claimantProvidedNoticeDate;
}

