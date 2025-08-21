package uk.gov.hmcts.reform.pcs.payment.serviceRequest.service;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.payment.fee.dto.FeeDto;
import uk.gov.hmcts.reform.pcs.payment.fee.entity.Fee;
import uk.gov.hmcts.reform.pcs.payment.serviceRequest.api.ServiceRequestApi;
import uk.gov.hmcts.reform.pcs.payment.serviceRequest.dto.CasePaymentRequestDto;
import uk.gov.hmcts.reform.pcs.payment.serviceRequest.model.ServiceRequestRequest;
import uk.gov.hmcts.reform.pcs.payment.serviceRequest.model.ServiceRequestResponse;

@Service
@Slf4j
public class ServiceRequestService {

    private static final String PAYMENT_ACTION = "payment";
    private static final String RESPONSIBLE_PARTY = "John Doe"; // Placeholder for a responsible party
    private static final int VOLUME = 1;

    private final ServiceRequestApi serviceRequestApi;
    private final String callBackUrl;

    public ServiceRequestService(
        @Autowired ServiceRequestApi serviceRequestApi,
        @Value("${payment.api.callback-url}") String callBackUrl
    ) {
        this.serviceRequestApi = serviceRequestApi;
        this.callBackUrl = callBackUrl;
    }

    public ServiceRequestResponse createServiceRequest(
        String authorisation,
        String serviceAuthorization,
        String caseReference,
        String ccdCaseNumber,
        Fee fee
    ) {
        try {
            return serviceRequestApi.createServiceRequest(
                authorisation,
                serviceAuthorization,
                ServiceRequestRequest.builder()
                    .callBackUrl(callBackUrl)
                    .casePaymentRequest(
                        CasePaymentRequestDto.builder()
                            .action(PAYMENT_ACTION)
                            .responsibleParty(RESPONSIBLE_PARTY)
                            .build()
                    )
                    .caseReference(String.valueOf(caseReference))
                    .ccdCaseNumber(String.valueOf(ccdCaseNumber))
                    .fees(new FeeDto[]{
                        FeeDto.builder()
                            .calculatedAmount(fee.getCalculatedAmount())
                            .code(fee.getCode())
                            .version(fee.getVersion())
                            .volume(VOLUME)
                            .build()
                    })
                    .build()
            );
        } catch (FeignException fe) {
            log.error(
                "Error in calling Payment Service Request API for case reference {} \n {}\n",
                ccdCaseNumber,
                fe.getMessage()
            );
            throw fe;
        }
    }
}
