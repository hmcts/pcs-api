package uk.gov.hmcts.reform.pcs.feesandpay.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.payments.client.models.CasePaymentRequestDto;
import uk.gov.hmcts.reform.payments.client.models.FeeDto;
import uk.gov.hmcts.reform.pcs.feesandpay.entity.Fee;

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
}
