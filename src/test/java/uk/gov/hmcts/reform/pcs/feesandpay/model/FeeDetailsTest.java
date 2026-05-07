package uk.gov.hmcts.reform.pcs.feesandpay.model;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fees.client.model.FeeLookupResponseDto;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FeeDetailsTest {

    @Test
    void shouldCreateInstanceFromFeeLookupResponse() {
        // Given
        String expectedCode = "f-123";
        String expectedDescription = "some description";
        BigDecimal expectedFeeAmount = new BigDecimal("250.12");
        int expectedVersion = 3;

        FeeLookupResponseDto feeLookupResponse = FeeLookupResponseDto.builder()
            .code(expectedCode)
            .description(expectedDescription)
            .feeAmount(expectedFeeAmount)
            .version(expectedVersion)
            .build();

        // When
        FeeDetails feeDetails = FeeDetails.fromFeeLookupResponse(feeLookupResponse);

        // Then
        assertThat(feeDetails.getCode()).isEqualTo(expectedCode);
        assertThat(feeDetails.getDescription()).isEqualTo(expectedDescription);
        assertThat(feeDetails.getFeeAmount()).isEqualTo(expectedFeeAmount);
        assertThat(feeDetails.getVersion()).isEqualTo(expectedVersion);
    }

}
