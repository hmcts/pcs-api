package uk.gov.hmcts.reform.pcs.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
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
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.feeandpay.FeePaymentRepository;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;
import uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentCallbackStrategy;
import uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentCallbackStrategyFactory;
import uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentService;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
    private static final long CASE_REFERENCE = 1234L;

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
    @MockitoBean
    private PaymentCallbackStrategyFactory paymentCallbackStrategyFactory;
    @Mock
    private PaymentCallbackStrategy pretendStrategy;

    private long caseReference;
    private String serviceCaseReference;
    private FeeDto feeDto;
    private FeesAndPayTaskData feesAndPayTaskData;
    private ClaimEntity claimEntity;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        serviceCaseReference = UUID.randomUUID().toString();
        feesAndPayTaskData = Instancio.create(FeesAndPayTaskData.class);
        feesAndPayTaskData.setCaseReference(CASE_REFERENCE);
        feesAndPayTaskData.setPaymentCallbackHandlerType(PaymentCallbackHandlerType.CLAIM);
        caseReference = feesAndPayTaskData.getCaseReference();
        feeDto = Instancio.create(FeeDto.class);
        feeDto.setCcdCaseNumber(String.valueOf(caseReference));
        PcsCaseEntity pcsCaseEntity = establishTestCase();
        claimEntity = pcsCaseEntity.getClaims().getFirst();
        ClaimPartyEntity claimPartyEntity = claimEntity.getClaimParties().getFirst();
        String orgName = claimPartyEntity.getParty().getOrgName();
        feesAndPayTaskData.setResponsibleParty(orgName);
        Mockito.when(paymentCallbackStrategyFactory.getStrategy(any())).thenReturn(pretendStrategy);
        establishFeePayment(serviceCaseReference);
    }

    @Test
    @Transactional
    void shouldProcessPaymentCallback() throws Exception {
        // Given
        PaymentStatusCallback paymentStatusCallback = Instancio.create(PaymentStatusCallback.class);
        paymentStatusCallback.setCcdCaseNumber(String.valueOf(caseReference));
        paymentStatusCallback.setServiceRequestReference(serviceCaseReference);
        paymentStatusCallback.setServiceRequestStatus(PaymentStatus.PAID.getValue());

        // When
        mockMvc.perform(put(PAYMENT_UPDATE_PATH)
                            .header(AUTHORIZATION, AUTH_HEADER)
                            .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(paymentStatusCallback)))
            .andExpect(status().isNoContent());

        // Then
        Optional<FeePaymentEntity> byRequestReference = feePaymentRepository
            .findByRequestReference(serviceCaseReference);
        assertThat(byRequestReference.isPresent()).isTrue();
        FeePaymentEntity feePaymentEntity = byRequestReference.get();
        assertThat(feePaymentEntity.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    }

    PcsCaseEntity establishTestCase() {
        return caseCreationHelper
            .createTestCaseWithParty(caseReference, null, PartyRole.CLAIMANT);
    }

    void establishFeePayment(String serviceCaseReference) throws JsonProcessingException {
        String asString = objectMapper.writeValueAsString(feesAndPayTaskData);
        paymentService.saveNewFeePayment(asString, feesAndPayTaskData, claimEntity, serviceCaseReference);
    }
}
