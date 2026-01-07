package uk.gov.hmcts.reform.pcs.feesandpay.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.payments.client.PaymentsClient;
import uk.gov.hmcts.reform.payments.client.models.CasePaymentRequestDto;
import uk.gov.hmcts.reform.payments.client.models.FeeDto;
import uk.gov.hmcts.reform.payments.request.CreateServiceRequestDTO;
import uk.gov.hmcts.reform.payments.response.PaymentServiceResponse;
import uk.gov.hmcts.reform.pcs.feesandpay.mapper.PaymentRequestMapper;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.idam.IdamService;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentsClient paymentsClient;

    @Mock
    private PaymentRequestMapper paymentRequestMapper;

    @Mock
    private IdamService idamService;

    @InjectMocks
    private PaymentService paymentService;

    @Captor
    private ArgumentCaptor<CreateServiceRequestDTO> createServiceRequestCaptor;

    @BeforeEach
    void setUp() throws Exception {
        var callbackUrlField = PaymentService.class.getDeclaredField("callbackUrl");
        callbackUrlField.setAccessible(true);
        callbackUrlField.set(paymentService, "https://callback");

        var hmctsOrgIdField = PaymentService.class.getDeclaredField("hmctsOrgId");
        hmctsOrgIdField.setAccessible(true);
        hmctsOrgIdField.set(paymentService, "TEST_ORG");
    }

    @Test
    void shouldCreateServiceRequestSuccessfully() {
        String caseReference = "BUS-123";
        String ccdCaseNumber = "1111-2222-3333-4444";
        int volume = 2;
        String responsibleParty = "Applicant";
        String systemToken = "Bearer sys-token";

        FeeDto mappedFee = FeeDto.builder()
            .calculatedAmount(new BigDecimal("808.00"))
            .code("FEE0412")
            .version("4")
            .volume(volume)
            .build();

        CasePaymentRequestDto casePaymentRequestDto = CasePaymentRequestDto.builder()
            .action("payment")
            .responsibleParty(responsibleParty)
            .build();

        PaymentServiceResponse paymentResponse = PaymentServiceResponse.builder()
            .serviceRequestReference("SR-123")
            .build();

        FeeDetails feeDetails = mock(FeeDetails.class);
        when(paymentRequestMapper.toFeeDto(feeDetails, volume)).thenReturn(mappedFee);
        when(paymentRequestMapper.toCasePaymentRequest(responsibleParty))
            .thenReturn(casePaymentRequestDto);
        when(idamService.getSystemUserAuthorisation()).thenReturn(systemToken);
        when(paymentsClient.createServiceRequest(eq(systemToken), any(CreateServiceRequestDTO.class)))
            .thenReturn(paymentResponse);

        PaymentServiceResponse result = paymentService.createServiceRequest(
            caseReference,
            ccdCaseNumber,
            feeDetails,
            volume,
            responsibleParty
        );

        assertThat(result).isNotNull();
        assertThat(result.getServiceRequestReference()).isEqualTo("SR-123");

        verify(paymentsClient).createServiceRequest(eq(systemToken), createServiceRequestCaptor.capture());
        CreateServiceRequestDTO sent = createServiceRequestCaptor.getValue();
        assertThat(sent.getCallBackUrl()).isEqualTo("https://callback");
        assertThat(sent.getHmctsOrgId()).isEqualTo("TEST_ORG");
        assertThat(sent.getCaseReference()).isEqualTo(caseReference);
        assertThat(sent.getCcdCaseNumber()).isEqualTo(ccdCaseNumber);
        assertThat(sent.getFees()).isNotNull();
        assertThat(sent.getFees()).hasSize(1);
        assertThat(sent.getFees()[0]).isEqualTo(mappedFee);
        assertThat(sent.getCasePaymentRequest()).isEqualTo(casePaymentRequestDto);
    }
}
