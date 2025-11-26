package uk.gov.hmcts.reform.pcs.feesandpay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "fees.mock", havingValue = "true")
public class FakeFeeService implements FeeService {

    @Override
    public FeeDetails getFee(String feeTypeKey) {
        return FeeDetails.builder()
            .code("FEE0123")
            .feeAmount(new BigDecimal("404.00"))
            .description("Fake fee")
            .version(1)
            .build();
    }

}
