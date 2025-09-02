package uk.gov.hmcts.reform.pcs.payment.servicerequest.service;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.idam.IdamService;
import uk.gov.hmcts.reform.pcs.payment.fee.dto.FeeDto;
import uk.gov.hmcts.reform.pcs.payment.fee.entity.Fee;
import uk.gov.hmcts.reform.pcs.payment.servicerequest.api.ServiceRequestApi;
import uk.gov.hmcts.reform.pcs.payment.servicerequest.dto.CasePaymentRequestDto;
import uk.gov.hmcts.reform.pcs.payment.servicerequest.model.ServiceRequestRequest;
import uk.gov.hmcts.reform.pcs.payment.servicerequest.model.ServiceRequestResponse;

@Service
@Slf4j
public class ServiceRequestService {

    private static final String PAYMENT_ACTION = "payment";
    private static final String RESPONSIBLE_PARTY = "John Doe"; // Placeholder for a responsible party
    private static final int VOLUME = 1;

    private final AuthTokenGenerator authTokenGenerator;
    private final IdamService idamService;
    private final ServiceRequestApi serviceRequestApi;
    private final String callBackUrl;

    public ServiceRequestService(
        @Autowired ServiceRequestApi serviceRequestApi,
        @Value("${payment.api.callback-url}") String callBackUrl,
        AuthTokenGenerator authTokenGenerator,
        IdamService idamService
    ) {
        this.authTokenGenerator = authTokenGenerator;
        this.idamService = idamService;
        this.serviceRequestApi = serviceRequestApi;
        this.callBackUrl = callBackUrl;
    }

    public ServiceRequestResponse createServiceRequest(
        String caseReference,
        String ccdCaseNumber,
        Fee fee
    ) {
        log.info("=== ServiceRequestService.createServiceRequest START ===");
        log.info("Input parameters - caseReference: {}, ccdCaseNumber: {}", caseReference, ccdCaseNumber);
        log.info("Input fee - code: {}, calculatedAmount: {}", fee.getCode(), fee.getCalculatedAmount());

        String serviceAuthToken = authTokenGenerator.generate();
        String systemUserAuth = idamService.getSystemUserAuthorisation();

        try {
            log.info("Step 1: Creating FeeDto...");
            FeeDto feeDto = FeeDto.builder()
                .calculatedAmount(fee.getCalculatedAmount())
                .code(fee.getCode())
                .version(fee.getVersion())
                .volume(VOLUME)
                .build();

            log.info("Step 2: FeeDto created successfully - calculatedAmount: {}", feeDto.getCalculatedAmount());

            log.info("Step 3: Creating CasePaymentRequestDto...");
            CasePaymentRequestDto casePaymentRequest = CasePaymentRequestDto.builder()
                .action(PAYMENT_ACTION)
                .responsibleParty(RESPONSIBLE_PARTY)
                .build();

            log.info("Step 4: CasePaymentRequestDto created successfully");

            log.info("Step 5: Creating ServiceRequestRequest...");
            ServiceRequestRequest request = ServiceRequestRequest.builder()
                .callBackUrl(callBackUrl)
                .casePaymentRequest(casePaymentRequest)
                .caseReference(String.valueOf(caseReference))
                .ccdCaseNumber(String.valueOf(ccdCaseNumber))
                .fees(new FeeDto[]{feeDto})
                .build();

            log.info("Step 6: ServiceRequestRequest created successfully");
            log.info("About to send request with {} fees", request.getFees().length);

            log.info("Step 7: Making API call...");
            ServiceRequestResponse response = serviceRequestApi.createServiceRequest(
                systemUserAuth,
                serviceAuthToken,
                request
            );

            log.info("Step 8: API call completed successfully");
            return response;

        } catch (Exception e) {
            log.error("Exception occurred in createServiceRequest: {}", e.getMessage(), e);
            if (e instanceof FeignException fe) {
                log.error("Feign error details - Status: {}, Body: {}", fe.status(), fe.contentUTF8());
            }
            throw e;
        }
    }
}
