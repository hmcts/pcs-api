package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.type.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TenancyLicence {

    private Boolean noticeServed;

    private BigDecimal rentAmount;

    private RentPaymentFrequency rentPaymentFrequency;

    private String otherRentFrequency;

    private BigDecimal dailyRentChargeAmount;

    private String tenancyLicenceType;

    private String detailsOfOtherTypeOfTenancyLicence;

    private LocalDate tenancyLicenceDate;

    private List<Document> supportingDocuments;

    private List<Document> rentStatementDocuments;

    private BigDecimal totalRentArrears;

    private List<ThirdPartyPaymentSource> thirdPartyPaymentSources;

    private String thirdPartyPaymentSourceOther;

}
