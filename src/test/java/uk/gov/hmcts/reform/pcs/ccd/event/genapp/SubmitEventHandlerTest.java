package uk.gov.hmcts.reform.pcs.ccd.event.genapp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.payments.response.PaymentServiceResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.CitizenGenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.MakeAnApplicationResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.XuiGenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.GenAppRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.LegalRepresentativeRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppDocumentGenerator;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppFeeCalculator;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.exception.PartyNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState.GEN_APP_ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState.PENDING_GEN_APP_ISSUED;
import static uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType.GEN_APP_ISSUE;


@ExtendWith(MockitoExtension.class)
class SubmitEventHandlerTest {

    private static final long TEST_CASE_REFERENCE = 1234L;

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private PartyService partyService;
    @Mock
    private SecurityContextService securityContextService;
    @Mock
    private GenAppService genAppService;
    @Mock
    private GenAppRepository genAppRepository;
    @Mock
    private GenAppDocumentGenerator genAppDocumentGenerator;
    @Mock
    private GenAppFeeCalculator genAppFeeCalculator;
    @Mock(strictness = LENIENT)
    private DocumentImportService documentImportService;
    @Mock
    private LegalRepresentativeRepository legalRepresentativeRepository;
    @Mock
    private ConfirmationScreenFactory confirmationScreenFactory;
    @Mock
    private PaymentService paymentService;
    @Mock
    private ObjectMapper objectMapper;

    private SubmitEventHandler underTest;

    @BeforeEach
    void setUp() {
        underTest = new SubmitEventHandler(pcsCaseService, partyService, securityContextService, genAppService,
                                           genAppRepository, genAppDocumentGenerator, genAppFeeCalculator,
                                           documentImportService, legalRepresentativeRepository,
                                           confirmationScreenFactory, paymentService, objectMapper
        );
    }

    @Nested
    @DisplayName("XUI submit event tests")
    class XuiSubmitEventTests {

        @Mock
        private PcsCaseEntity pcsCaseEntity;

        @BeforeEach
        void setUp() {
            stubDocumentImport();
            given(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).willReturn(pcsCaseEntity);
        }

        @Test
        void shouldCreateGenAppWithCaseDataAndRepresentedParty() {
            // Given
            UUID representedPartyUuid = UUID.randomUUID();
            PartyEntity representedParty = mock(PartyEntity.class);

            XuiGenAppRequest genAppRequest = XuiGenAppRequest.builder()
                .applicationType(GenAppType.SET_ASIDE)
                .build();

            when(partyService.getPartyEntityByEntityId(representedPartyUuid, TEST_CASE_REFERENCE))
                .thenReturn(representedParty);

            stubLegalRepForParty(representedPartyUuid);

            stubCreateGenAppEntity(genAppRequest, pcsCaseEntity, representedParty);

            stubApplicationFeeCalculation(genAppRequest);

            PCSCase caseData = PCSCase.builder()
                .currentRepresentedPartyId(representedPartyUuid.toString())
                .xuiGenAppRequest(genAppRequest)
                .build();

            // When
            underTest.submit(eventPayload(caseData));

            // Then
            verify(genAppService)
                .createGenAppEntity(genAppRequest, pcsCaseEntity, representedParty, PENDING_GEN_APP_ISSUED);
        }

        @Test
        void shouldBuildConfirmationScreenResponse() {
            // Given
            UUID representedPartyUuid = UUID.randomUUID();
            PartyEntity representedParty = mock(PartyEntity.class);

            XuiGenAppRequest genAppRequest = XuiGenAppRequest.builder()
                .applicationType(GenAppType.SET_ASIDE)
                .build();

            when(partyService.getPartyEntityByEntityId(representedPartyUuid, TEST_CASE_REFERENCE))
                .thenReturn(representedParty);

            stubLegalRepForParty(representedPartyUuid);

            stubCreateGenAppEntity(genAppRequest, pcsCaseEntity, representedParty);

            FeeDetails feeDetails = stubApplicationFeeCalculation(genAppRequest);

            SubmitResponse<State> expectedSubmitResponse = createMockSubmitResponse();
            when(confirmationScreenFactory
                     .buildConfirmationScreenResponse(genAppRequest, TEST_CASE_REFERENCE, feeDetails))
                .thenReturn(expectedSubmitResponse);

            PCSCase caseData = PCSCase.builder()
                .currentRepresentedPartyId(representedPartyUuid.toString())
                .xuiGenAppRequest(genAppRequest)
                .build();

            // When
            SubmitResponse<State> actualSubmitResponse = underTest.submit(eventPayload(caseData));

            // Then
            assertThat(actualSubmitResponse).isEqualTo(expectedSubmitResponse);
        }

        @Test
        void shouldThrowErrorIfApplicantIsNotRepresentedByCurrentUser() {
            // Given
            UUID representedPartyUuid = UUID.randomUUID();
            XuiGenAppRequest genAppRequest = XuiGenAppRequest.builder()
                .applicationType(GenAppType.SET_ASIDE)
                .build();

            PCSCase caseData = PCSCase.builder()
                .currentRepresentedPartyId(representedPartyUuid.toString())
                .xuiGenAppRequest(genAppRequest)
                .build();

            UUID currentUserId = UUID.randomUUID();
            when(securityContextService.getCurrentUserId()).thenReturn(currentUserId);

            when(legalRepresentativeRepository
                     .isLegalRepresentativeLinkedToPartyAndActive(currentUserId, representedPartyUuid))
                .thenReturn(false);

            // When
            Throwable throwable = catchThrowable(() -> underTest.submit(eventPayload(caseData)));

            // Then
            assertThat(throwable).isInstanceOf(PartyNotFoundException.class);
        }

        private void stubLegalRepForParty(UUID representedPartyUuid) {
            UUID currentUserId = UUID.randomUUID();
            when(securityContextService.getCurrentUserId()).thenReturn(currentUserId);
            when(legalRepresentativeRepository
                     .isLegalRepresentativeLinkedToPartyAndActive(currentUserId, representedPartyUuid))
                .thenReturn(true);
        }

    }

    private FeeDetails stubApplicationFeeCalculation(GenAppRequest genAppRequest) {
        BigDecimal applicationFee = new BigDecimal("55.00");
        FeeDetails feeDetails = FeeDetails.builder()
            .feeAmount(applicationFee)
            .build();
        when(genAppFeeCalculator.getApplicationFeeDetails(genAppRequest)).thenReturn(Optional.of(feeDetails));
        return feeDetails;
    }

    @Nested
    @DisplayName("Citizen submit event tests")
    class CitizenSubmitEventTests {

        @Mock
        private PcsCaseEntity pcsCaseEntity;

        @BeforeEach
        void setUp() {
            stubDocumentImport();
            given(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).willReturn(pcsCaseEntity);
        }

        @Test
        void shouldCreateGenAppWithCaseDataAndApplicantPartyWhenNoFeeDue() {
            // Given
            final PartyEntity applicantParty = mock(PartyEntity.class);

            CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
                .applicationType(GenAppType.SET_ASIDE)
                .clientReference("some reference")
                .build();

            stubCreateGenAppEntity(genAppRequest, pcsCaseEntity, applicantParty);

            UUID currentUserId = UUID.randomUUID();
            given(securityContextService.getCurrentUserId()).willReturn(currentUserId);
            given(partyService.getPartyEntityByIdamId(currentUserId, TEST_CASE_REFERENCE)).willReturn(applicantParty);

            when(genAppFeeCalculator.getApplicationFeeDetails(genAppRequest)).thenReturn(Optional.empty());

            PCSCase caseData = PCSCase.builder()
                .citizenGenAppRequest(genAppRequest)
                .build();

            // When
            underTest.submit(eventPayload(caseData));

            // Then
            verify(genAppService)
                .createGenAppEntity(genAppRequest, pcsCaseEntity, applicantParty, GEN_APP_ISSUED);
        }

        @Test
        void shouldReturnErrorIfDuplicateClientReference() {
            // Given
            String existingClientReference = "ref-1234";
            CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
                .applicationType(GenAppType.ADJOURN)
                .clientReference(existingClientReference)
                .build();

            PCSCase caseData = PCSCase.builder()
                .citizenGenAppRequest(genAppRequest)
                .build();

            when(genAppRepository.existsByPcsCaseAndClientReference(pcsCaseEntity, existingClientReference))
                .thenReturn(true);

            // When
            SubmitResponse<State> submitResponse = underTest.submit(eventPayload(caseData));

            // Then
            assertThat(submitResponse.getErrors())
                .containsExactly("Application already exists for client reference");

            verify(genAppService, never()).createGenAppEntity(any(), any(), any(), any());
        }

        @Test
        void shouldGenerateGenAppDocumentAndStoreMetadata() {
            CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
                .applicationType(GenAppType.ADJOURN)
                .clientReference("some reference")
                .build();

            PCSCase caseData = PCSCase.builder()
                .citizenGenAppRequest(genAppRequest)
                .build();

            PartyEntity applicantParty = stubCurrentUserParty();

            GenAppEntity genAppEntity = stubCreateGenAppEntity(genAppRequest, pcsCaseEntity, applicantParty);

            String documentUrl = "some document URL";
            when(genAppDocumentGenerator
                     .generateSubmissionDocument(TEST_CASE_REFERENCE, genAppRequest, genAppEntity, applicantParty))
                .thenReturn(documentUrl);

            // When
            underTest.submit(eventPayload(caseData));

            // Then
            verify(documentImportService).addDocumentToCase(TEST_CASE_REFERENCE, documentUrl,
                                                            CaseFileCategory.APPLICATIONS
            );
        }

        @Test
        void shouldSetGenAppReferenceOnImportedDocumentEntity() {
            CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
                .applicationType(GenAppType.ADJOURN)
                .clientReference("some reference")
                .build();

            final PCSCase caseData = PCSCase.builder()
                .citizenGenAppRequest(genAppRequest)
                .build();

            PartyEntity applicantParty = stubCurrentUserParty();

            GenAppEntity genAppEntity = stubCreateGenAppEntity(genAppRequest, pcsCaseEntity, applicantParty);

            String documentUrl = "some document URL";
            when(genAppDocumentGenerator
                     .generateSubmissionDocument(TEST_CASE_REFERENCE, genAppRequest, genAppEntity, applicantParty))
                .thenReturn(documentUrl);

            DocumentEntity importedDocumentEntity = stubDocumentImport();

            // When
            underTest.submit(eventPayload(caseData));

            // Then
            verify(importedDocumentEntity).setGeneralApplication(genAppEntity);
            verify(genAppEntity).setSubmissionDocument(importedDocumentEntity);
        }

        @Test
        void shouldCreateServiceRequestWhenFeeDue() throws JsonProcessingException {
            // Given
            final PartyEntity applicantParty = mock(PartyEntity.class);
            UUID applicantPartyId = UUID.randomUUID();
            when(applicantParty.getId()).thenReturn(applicantPartyId);

            CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
                .applicationType(GenAppType.SET_ASIDE)
                .clientReference("some reference")
                .build();

            UUID expectedGenAppEntityId = UUID.randomUUID();
            GenAppEntity genAppEntity = stubCreateGenAppEntity(genAppRequest, pcsCaseEntity, applicantParty);
            when(genAppEntity.getId()).thenReturn(expectedGenAppEntityId);

            UUID currentUserId = UUID.randomUUID();
            given(securityContextService.getCurrentUserId()).willReturn(currentUserId);
            given(partyService.getPartyEntityByIdamId(currentUserId, TEST_CASE_REFERENCE)).willReturn(applicantParty);

            stubApplicationFeeCalculation(genAppRequest);
            stubPaymentServiceResponse();

            when(objectMapper.writeValueAsString(any(MakeAnApplicationResponse.class)))
                .thenReturn("serialised response JSON");

            PCSCase caseData = PCSCase.builder()
                .citizenGenAppRequest(genAppRequest)
                .build();

            // When
            underTest.submit(eventPayload(caseData));

            // Then
            ArgumentCaptor<FeesAndPayTaskData> feesAndPayTaskDataCaptor
                = ArgumentCaptor.forClass(FeesAndPayTaskData.class);

            verify(paymentService).createServiceRequest(feesAndPayTaskDataCaptor.capture());

            FeesAndPayTaskData feesAndPayTaskData = feesAndPayTaskDataCaptor.getValue();
            assertThat(feesAndPayTaskData.getCcdCaseNumber()).isEqualTo(Long.toString(TEST_CASE_REFERENCE));
            assertThat(feesAndPayTaskData.getCaseReference()).isEqualTo(TEST_CASE_REFERENCE);
            assertThat(feesAndPayTaskData.getResponsiblePartyId()).isEqualTo(applicantPartyId);
            assertThat(feesAndPayTaskData.getPaymentCallbackHandlerType()).isEqualTo(GEN_APP_ISSUE);
            assertThat(feesAndPayTaskData.getRelatedEntityId()).isEqualTo(expectedGenAppEntityId);
        }

        @Test
        void shouldSetGenAppStateAsPendingAndReturnPaymentDetailsWhenFeeDue() throws JsonProcessingException {
            // Given
            final PartyEntity applicantParty = mock(PartyEntity.class);

            CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
                .applicationType(GenAppType.SET_ASIDE)
                .clientReference("some reference")
                .build();

            stubCreateGenAppEntity(genAppRequest, pcsCaseEntity, applicantParty);
            final String expectedServiceRequestReference = stubPaymentServiceResponse();

            UUID currentUserId = UUID.randomUUID();
            given(securityContextService.getCurrentUserId()).willReturn(currentUserId);
            given(partyService.getPartyEntityByIdamId(currentUserId, TEST_CASE_REFERENCE)).willReturn(applicantParty);

            final FeeDetails feeDetails = stubApplicationFeeCalculation(genAppRequest);

            when(objectMapper.writeValueAsString(any(MakeAnApplicationResponse.class)))
                .thenReturn("serialised response JSON");

            PCSCase caseData = PCSCase.builder()
                .citizenGenAppRequest(genAppRequest)
                .build();

            // When
            SubmitResponse<State> actualSubmitResponse = underTest.submit(eventPayload(caseData));

            // Then
            verifyNoInteractions(genAppDocumentGenerator);

            String confirmationBody = actualSubmitResponse.getConfirmationBody();
            assertThat(confirmationBody).isEqualTo("serialised response JSON");
            ArgumentCaptor<MakeAnApplicationResponse> makeAnApplicationResponseCaptor
                = ArgumentCaptor.forClass(MakeAnApplicationResponse.class);

            verify(objectMapper).writeValueAsString(makeAnApplicationResponseCaptor.capture());
            MakeAnApplicationResponse makeAnApplicationResponse = makeAnApplicationResponseCaptor.getValue();

            assertThat(makeAnApplicationResponse.getState()).isEqualTo(PENDING_GEN_APP_ISSUED);
            assertThat(makeAnApplicationResponse.getServiceRequestReference())
                .isEqualTo(expectedServiceRequestReference);
            assertThat(makeAnApplicationResponse.getFeeAmount()).isEqualTo(feeDetails.getFeeAmount());
        }

        private String stubPaymentServiceResponse() {
            PaymentServiceResponse paymentResponse = mock(PaymentServiceResponse.class);
            when(paymentService.createServiceRequest(any(FeesAndPayTaskData.class))).thenReturn(paymentResponse);
            String expectedServiceRequestReference = "SR-1234";
            when(paymentResponse.getServiceRequestReference()).thenReturn(expectedServiceRequestReference);
            return expectedServiceRequestReference;
        }

    }

    private static EventPayload<PCSCase, State> eventPayload(PCSCase caseData) {
        return new EventPayload<>(TEST_CASE_REFERENCE, caseData, null);
    }

    private PartyEntity stubCurrentUserParty() {
        PartyEntity currentUserParty = mock(PartyEntity.class);
        UUID currentUserId = UUID.randomUUID();
        given(securityContextService.getCurrentUserId()).willReturn(currentUserId);
        given(partyService.getPartyEntityByIdamId(currentUserId, TEST_CASE_REFERENCE)).willReturn(currentUserParty);
        return currentUserParty;
    }

    private DocumentEntity stubDocumentImport() {
        DocumentEntity importedDocumentEntity = mock(DocumentEntity.class);
        when(documentImportService
                 .addDocumentToCase(eq(TEST_CASE_REFERENCE), nullable(String.class), any(CaseFileCategory.class)))
            .thenReturn(importedDocumentEntity);
        return importedDocumentEntity;
    }

    private GenAppEntity stubCreateGenAppEntity(GenAppRequest genAppRequest,
                                                PcsCaseEntity pcsCaseEntity,
                                                PartyEntity applicantParty) {
        GenAppEntity genAppEntity = mock(GenAppEntity.class);
        when(genAppService
                 .createGenAppEntity(eq(genAppRequest), eq(pcsCaseEntity), eq(applicantParty), any(GenAppState.class)))
            .thenReturn(genAppEntity);
        return genAppEntity;
    }

    @SuppressWarnings("unchecked")
    private static SubmitResponse<State> createMockSubmitResponse() {
        return mock(SubmitResponse.class);
    }

}
