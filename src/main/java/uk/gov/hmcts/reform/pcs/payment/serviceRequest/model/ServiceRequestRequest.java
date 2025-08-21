package uk.gov.hmcts.reform.pcs.payment.serviceRequest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.pcs.payment.serviceRequest.dto.CasePaymentRequestDto;
import uk.gov.hmcts.reform.pcs.payment.fee.dto.FeeDto;

@Data
@Builder
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceRequestRequest {
    @JsonProperty("call_back_url")
    private String callBackUrl;
    @JsonProperty("case_payment_request")
    private CasePaymentRequestDto casePaymentRequest;
    @JsonProperty("case_reference")
    private String caseReference;
    @JsonProperty("ccd_case_number")
    private String ccdCaseNumber;
    @JsonProperty("fees")
    private FeeDto[] fees;
    @Builder.Default
    @JsonProperty("hmcts_org_id")
    private String hmctsOrgId = "BFA1"; // IA Case Payments Organisation ID, used for testing purposes
}
