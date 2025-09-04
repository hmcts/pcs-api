package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
// Represents the tenancy licence details for a possessions case.
// This class is a placeholder for future fields that may be added as the data model evolves.
public class TenancyLicence {

    private Boolean noticeServed;
    
    private BigDecimal rentAmount;
    
    private RentPaymentFrequency rentPaymentFrequency;
    
    private String otherRentFrequency;
    
    private BigDecimal dailyRentChargeAmount;

    // Notice details fields
    private String noticeServiceMethod;
    private LocalDate noticePostedDate;
    private LocalDate noticeDeliveredDate;
    private LocalDateTime noticeHandedOverDateTime;
    private String noticePersonName;
    private LocalDateTime noticeEmailSentDateTime;
    private String noticeEmailExplanation;
    private LocalDateTime noticeOtherElectronicDateTime;
    private LocalDateTime noticeOtherDateTime;
    private String noticeOtherExplanation;
}
