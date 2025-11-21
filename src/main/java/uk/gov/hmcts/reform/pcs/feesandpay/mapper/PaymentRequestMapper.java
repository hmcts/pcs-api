package uk.gov.hmcts.reform.pcs.feesandpay.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.payments.client.models.CasePaymentRequestDto;
import uk.gov.hmcts.reform.payments.client.models.FeeDto;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;

@Component
public class PaymentRequestMapper {

    private static final String ACTION_PAYMENT = "payment";

    /**
     * Maps a Fees API response into a Payments API FeeDto, adding the requested volume.
     *
     * @param feeDetails the fee details
     * @param volume the quantity to apply
     * @return a FeeDto suitable for Payments API requests
     */
    public FeeDto toFeeDto(FeeDetails feeDetails, int volume) {
        if (feeDetails == null) {
            throw new IllegalArgumentException("fee details must not be null");
        }

        return FeeDto.builder()
            .code(feeDetails.getCode())
            .calculatedAmount(feeDetails.getFeeAmount())
            .version(String.valueOf(feeDetails.getVersion()))
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
