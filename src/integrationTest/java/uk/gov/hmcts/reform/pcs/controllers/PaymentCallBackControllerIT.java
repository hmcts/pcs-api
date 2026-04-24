package uk.gov.hmcts.reform.pcs.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.payments.client.models.FeeDto;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.feeandpay.FeePaymentRepository;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.model.ServiceRequestUpdate;
import uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentService;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.pcs.feesandpay.endpoint.PaymentCallBackController.PAYMENT_UPDATE_PATH;
import static uk.gov.hmcts.reform.pcs.feesandpay.endpoint.PaymentCallBackController.SERVICE_AUTHORIZATION;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@DisplayName("PaymentCallBackController Integration Tests")
public class PaymentCallBackControllerIT extends AbstractPostgresContainerIT {

    private static final String AUTH_HEADER = "Bearer test-token";
    private static final String SERVICE_AUTH_HEADER = "ServiceAuthToken";

    @MockitoBean
    private IdamClient idamClient;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private CaseCreationHelper caseCreationHelper;
    @Autowired
    private FeePaymentRepository feePaymentRepository;

    private long caseReference = 12345L;
    private String serviceCaseReference;
    private FeeDto feeDto;

    @BeforeEach
    void setUp() {
        serviceCaseReference = UUID.randomUUID().toString();
        feeDto = Instancio.create(FeeDto.class);
        feeDto.setCcdCaseNumber(String.valueOf(caseReference));
        PcsCaseEntity pcsCaseEntity = establishTestCase(caseReference);
        establishFeePayment(pcsCaseEntity, serviceCaseReference, feeDto);
    }

    @Test
    @Transactional
    void shouldProcessPaymentCallback() throws Exception {
        // Given
        ServiceRequestUpdate serviceRequestUpdate = Instancio.create(ServiceRequestUpdate.class);
        serviceRequestUpdate.setCcdCaseNumber(String.valueOf(caseReference));
        serviceRequestUpdate.setServiceRequestReference(serviceCaseReference);
        serviceRequestUpdate.setServiceRequestStatus(PaymentStatus.PAID.getValue());

        // When
        mockMvc.perform(put(PAYMENT_UPDATE_PATH)
                            .header(AUTHORIZATION, AUTH_HEADER)
                            .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(serviceRequestUpdate)))
            .andExpect(status().isOk());

        // Then
        Optional<FeePaymentEntity> byRequestReference = feePaymentRepository
            .findByRequestReference(serviceCaseReference);
        assertThat(byRequestReference.isPresent()).isTrue();
        FeePaymentEntity feePaymentEntity = byRequestReference.get();
        assertThat(feePaymentEntity.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    }

    PcsCaseEntity establishTestCase(long caseReference) {
        return caseCreationHelper
            .createTestCaseWithParty(caseReference, null, PartyRole.DEFENDANT);
    }

    void establishFeePayment(PcsCaseEntity pcsCaseEntity, String serviceCaseReference, FeeDto feeDto) {
        ClaimEntity claimEntity = pcsCaseEntity.getClaims().getFirst();
        paymentService.saveNewFeePayment(String.valueOf(caseReference),
                                         claimEntity, feeDto, serviceCaseReference);
    }
}
