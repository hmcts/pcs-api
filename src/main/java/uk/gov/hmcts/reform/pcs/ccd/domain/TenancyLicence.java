package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TenancyLicence {

    private Boolean noticeServed;

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

    private List<Document> supportingDocuments;

    private Boolean arrearsJudgmentWanted;

    private List<Document> rentStatementDocuments;

    private List<Document> noticeDocuments;

    private BigDecimal totalRentArrears;

    private List<ThirdPartyPaymentSource> thirdPartyPaymentSources;

    private String thirdPartyPaymentSourceOther;

    // Wales Housing Act details
    private YesNoNotApplicable walesRegistered;
    private String walesRegistrationNumber;
    private YesNoNotApplicable walesLicensed;
    private String walesLicenceNumber;
    private YesNoNotApplicable walesLicensedAgentAppointed;
    private String walesAgentFirstName;
    private String walesAgentLastName;
    private String walesAgentLicenceNumber;
    private LocalDate walesAgentAppointmentDate;

    //Wales notice details
    private Boolean walesNoticeServed;
    private String walesTypeOfNoticeServed;

    // Wales Occupation Contract/Licence details
    private OccupationLicenceTypeWales occupationLicenceTypeWales;
    private String walesOtherLicenceTypeDetails;
    private LocalDate walesLicenceStartDate;
    private List<Document> walesLicenceDocuments;

}
