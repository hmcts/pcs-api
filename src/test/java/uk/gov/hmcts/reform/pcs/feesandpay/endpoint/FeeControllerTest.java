package uk.gov.hmcts.reform.pcs.feesandpay.endpoint;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pcs.controllers.RestExceptionHandler;
import uk.gov.hmcts.reform.pcs.feesandpay.exception.FeeNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.ClaimIssueFeeResponse;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;
import uk.gov.hmcts.reform.pcs.feesandpay.service.FeeService;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeeControllerTest {

    private static final String S2S_TOKEN = "service-token";

    @Mock
    private FeeService feeService;

    @InjectMocks
    private FeeController underTest;

    @Test
    void shouldReturnClaimIssueFeeWithTwoDecimalPlaces() {
        when(feeService.getFee(FeeType.CASE_ISSUE_FEE)).thenReturn(fee(new BigDecimal("404")));

        ResponseEntity<?> response = underTest.getClaimIssueFee(S2S_TOKEN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(new ClaimIssueFeeResponse("404.00", "FEE0412", 4));
    }

    @Test
    void shouldKeepPenceInTheAmount() {
        when(feeService.getFee(FeeType.CASE_ISSUE_FEE)).thenReturn(fee(new BigDecimal("404.5")));

        ResponseEntity<?> response = underTest.getClaimIssueFee(S2S_TOKEN);

        assertThat(((ClaimIssueFeeResponse) response.getBody()).amount()).isEqualTo("404.50");
    }

    @Test
    void shouldReturn503WhenTheFeeCannotBeRetrieved() {
        when(feeService.getFee(FeeType.CASE_ISSUE_FEE)).thenThrow(new FeeNotFoundException("down"));

        ResponseEntity<?> response = underTest.getClaimIssueFee(S2S_TOKEN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isEqualTo(new RestExceptionHandler.Error("The claim fee is unavailable"));
    }

    private static FeeDetails fee(BigDecimal amount) {
        return FeeDetails.builder().code("FEE0412").version(4).feeAmount(amount).build();
    }
}
