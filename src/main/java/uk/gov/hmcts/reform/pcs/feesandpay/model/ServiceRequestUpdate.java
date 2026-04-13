package uk.gov.hmcts.reform.pcs.feesandpay.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceRequestUpdate {

    @JsonProperty("service_request_reference")
    private String serviceRequestReference;
    @JsonProperty("ccd_case_number")
    private String ccdCaseNumber;
    @JsonProperty("service_request_amount")
    private BigDecimal serviceRequestAmount;
    @JsonProperty("service_request_status")
    private String serviceRequestStatus;
    @JsonProperty("payment")
    private Payment payment;

}
