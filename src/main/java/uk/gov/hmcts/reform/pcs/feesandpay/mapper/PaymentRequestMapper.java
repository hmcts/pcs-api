package uk.gov.hmcts.reform.pcs.feesandpay.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.fees.client.model.FeeLookupResponseDto;
import uk.gov.hmcts.reform.payments.client.models.CasePaymentRequestDto;
import uk.gov.hmcts.reform.payments.client.models.FeeDto;

@Component
public class PaymentRequestMapper {

    private static final String ACTION_PAYMENT = "payment";

    /**
     * Maps a Fees API response into a Payments API FeeDto, adding the requested volume.
     *
     * @param fee the fee returned by the Fees API
     * @param volume the quantity to apply
     * @return a FeeDto suitable for Payments API requests
     */
    public FeeDto toFeeDto(FeeLookupResponseDto fee, int volume) {
        if (fee == null) {
            throw new IllegalArgumentException("fee must not be null");
        }

        return FeeDto.builder()
            .code(fee.getCode())
            .calculatedAmount(fee.getFeeAmount())
            .version(String.valueOf(fee.getVersion()))
            .volume(volume)
            .build();
    }

    /**
     * Builds a CasePaymentRequestDto with the provided parameters.
     *
     * @param responsibleParty the responsible party
     * @return a CasePaymentRequestDto instance
     */
    public CasePaymentRequestDto toCasePaymentRequest(String responsibleParty) {
        return CasePaymentRequestDto.builder()
            .action(ACTION_PAYMENT)
            .responsibleParty(responsibleParty)
            .build();
    }
}
