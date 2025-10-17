package uk.gov.hmcts.reform.pcs.feesandpay.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.feesandpay.dto.CasePaymentRequestDto;
import uk.gov.hmcts.reform.pcs.feesandpay.dto.FeeDto;
import uk.gov.hmcts.reform.pcs.feesandpay.entity.Fee;
import uk.gov.hmcts.reform.pcs.feesandpay.model.ServiceRequestBody;

@Component
public class PaymentRequestMapper {

    public FeeDto toFeeDto(Fee fee, int volume) {
        return FeeDto.builder()
            .calculatedAmount(fee.getCalculatedAmount())
            .code(fee.getCode())
            .version(fee.getVersion())
            .volume(volume)
            .build();
    }

    public CasePaymentRequestDto toCasePaymentRequest(String action, String responsibleParty) {
        return CasePaymentRequestDto.builder()
            .action(action)
            .responsibleParty(responsibleParty)
            .build();
    }

    public ServiceRequestBody toServiceRequestBody(
        String callbackUrl,
        String caseReference,
        String ccdCaseNumber,
        FeeDto[] fees,
        CasePaymentRequestDto casePaymentRequest
    ) {
        return ServiceRequestBody.builder()
            .callbackUrl(callbackUrl)
            .casePaymentRequest(casePaymentRequest)
            .caseReference(caseReference)
            .ccdCaseNumber(ccdCaseNumber)
            .fees(fees)
            .build();
    }
}
