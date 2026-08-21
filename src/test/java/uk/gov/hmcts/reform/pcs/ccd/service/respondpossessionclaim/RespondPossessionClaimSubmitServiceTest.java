package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.camunda.CamundaService;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.UploadedDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaim;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimType;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.CounterClaimTaskData;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentService;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TranslationWAService;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TaskDescriptionService;
import uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.model.JourneyType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;
import static uk.gov.hmcts.reform.pcs.ccd.task.PendingCounterClaimIssuedNotificationTaskComponent.PENDING_COUNTER_CLAIM_ISSUED_TASK_DESCRIPTOR;

@ExtendWith(MockitoExtension.class)
class RespondPossessionClaimSubmitServiceTest {

    private static final long CASE_REFERENCE = 1234567890123456L;

    @Mock
    private ClaimResponseService claimResponseService;
    @Mock
    private DefendantResponseService defendantResponseService;
    @Mock
    private CounterClaimService counterClaimService;
    @Mock
    private CounterClaimFeeCalculator counterClaimFeeCalculator;
    @Mock
    private DocumentService documentService;
    @Mock
    private DraftCaseDataService draftCaseDataService;
    @Mock
    private TranslationWAService translationWAService;
    @Mock
    private TaskDescriptionService taskDescriptionService;
    @Mock
    private CamundaService camundaService;
    @Mock
    private PartyEntity partyEntity;
    @Mock
    private SchedulerClient schedulerClient;
    @Captor
    private ArgumentCaptor<SchedulableInstance<CounterClaimTaskData>> schedulableInstanceCaptor;

    private RespondPossessionClaimSubmitService underTest;

    @BeforeEach
    void setUp() {
        underTest = new RespondPossessionClaimSubmitService(
            claimResponseService,
            defendantResponseService,
            counterClaimService,
            counterClaimFeeCalculator,
            documentService,
            draftCaseDataService,
            schedulerClient,
            taskDescriptionService,
            camundaService,
            translationWAService
        );

        when(defendantResponseService.saveDefendantResponse(anyLong(), any(), any(), any()))
            .thenReturn(DefendantResponseEntity.builder().languageUsed(LanguageUsed.ENGLISH).build());
    }

    @Test
    void shouldPersistResponseWithoutCounterClaimCitizen() {
        this.shouldPersistResponseWithoutCounterClaim(JourneyType.CITIZEN);
        verify(draftCaseDataService).deleteUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim);
    }

    @Test
    void shouldPersistResponseWithoutCounterClaimLegalRepresentative() {
        when(partyEntity.getId()).thenReturn(UUID.randomUUID());

        this.shouldPersistResponseWithoutCounterClaim(JourneyType.LEGAL_REPRESENTATIVE);
        verify(draftCaseDataService)
            .deleteUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim, partyEntity.getId());
    }

    void shouldPersistResponseWithoutCounterClaim(JourneyType journeyType) {
        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(DefendantResponses.builder().build())
            .build();

        when(counterClaimService.saveCounterClaim(CASE_REFERENCE, null, partyEntity)).thenReturn(Optional.empty());

        RespondPossessionClaimSubmitPersistenceResult result =
            underTest.persistFinalSubmit(CASE_REFERENCE, possessionClaimResponse, partyEntity, journeyType);

        verify(claimResponseService).saveDraftDataForParty(possessionClaimResponse, partyEntity, CASE_REFERENCE);
        verify(defendantResponseService)
            .saveDefendantResponse(CASE_REFERENCE, possessionClaimResponse, partyEntity, journeyType);
        verify(documentService, never()).createCounterClaimUploadedDocuments(any(), any(), any(), any());
        assertThat(result.counterClaimEntity()).isNull();
        assertThat(result.paymentRequired()).isFalse();
        assertThat(result.possessionClaimResponse()).isEqualTo(possessionClaimResponse);
    }

    @Test
    void shouldPersistCounterClaimAndCreatePaymentWhenFeeIsRequiredForCitizenJourney() {
        JourneyType journeyType = JourneyType.CITIZEN;

        CounterClaim counterClaim = CounterClaim.builder()
            .claimType(CounterClaimType.PAYMENT_OR_COMPENSATION)
            .isClaimAmountKnown(VerticalYesNo.YES)
            .claimAmount(new BigDecimal("2500.00"))
            .build();
        DefendantResponses defendantResponses = DefendantResponses.builder()
            .counterClaim(counterClaim)
            .build();
        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(defendantResponses)
            .build();
        CounterClaimEntity savedCounterClaim = CounterClaimEntity.builder()
            .id(UUID.randomUUID())
            .status(CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED)
            .build();

        when(counterClaimService.saveCounterClaim(CASE_REFERENCE, counterClaim, partyEntity))
            .thenReturn(Optional.of(savedCounterClaim));
        when(counterClaimFeeCalculator.isHwfReferencePresent(counterClaim)).thenReturn(false);
        FeeDetails expectedFeeDetails = mock(FeeDetails.class);
        when(counterClaimFeeCalculator.getFeeDetails(counterClaim)).thenReturn(expectedFeeDetails);

        RespondPossessionClaimSubmitPersistenceResult result =
            underTest.persistFinalSubmit(CASE_REFERENCE, possessionClaimResponse, partyEntity, journeyType);

        verify(draftCaseDataService).deleteUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim);
        assertThat(result.counterClaimEntity()).isEqualTo(savedCounterClaim);
        assertThat(result.paymentRequired()).isTrue();
        assertThat(result.feeDetails()).isEqualTo(expectedFeeDetails);
        verifyNoInteractions(camundaService);
    }

    @Test
    void shouldPersistCounterClaimAndCreatePaymentWhenFeeIsRequiredForLegalRepJourney() {
        JourneyType journeyType = JourneyType.LEGAL_REPRESENTATIVE;

        CounterClaim counterClaim = CounterClaim.builder()
            .claimType(CounterClaimType.PAYMENT_OR_COMPENSATION)
            .isClaimAmountKnown(VerticalYesNo.YES)
            .claimAmount(new BigDecimal("2500.00"))
            .build();
        DefendantResponses defendantResponses = DefendantResponses.builder()
            .counterClaim(counterClaim)
            .build();
        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(defendantResponses)
            .build();
        CounterClaimEntity savedCounterClaim = CounterClaimEntity.builder()
            .id(UUID.randomUUID())
            .status(CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED)
            .build();

        when(partyEntity.getId()).thenReturn(UUID.randomUUID());
        when(counterClaimService.saveCounterClaim(CASE_REFERENCE, counterClaim, partyEntity))
            .thenReturn(Optional.of(savedCounterClaim));
        when(counterClaimFeeCalculator.isHwfReferencePresent(counterClaim)).thenReturn(false);
        FeeDetails expectedFeeDetails = mock(FeeDetails.class);
        when(counterClaimFeeCalculator.getFeeDetails(counterClaim)).thenReturn(expectedFeeDetails);

        RespondPossessionClaimSubmitPersistenceResult result =
            underTest.persistFinalSubmit(CASE_REFERENCE, possessionClaimResponse, partyEntity, journeyType);

        verify(draftCaseDataService).deleteUnsubmittedCaseData(
            CASE_REFERENCE, respondPossessionClaim, partyEntity.getId());
        assertThat(result.counterClaimEntity()).isEqualTo(savedCounterClaim);
        assertThat(result.paymentRequired()).isTrue();
        assertThat(result.feeDetails()).isEqualTo(expectedFeeDetails);
    }

    @ParameterizedTest
    @EnumSource(JourneyType.class)
    void shouldNotIssueCounterClaimWhenHelpWithFeesApplies(JourneyType journeyType) {
        CounterClaim counterClaim = CounterClaim.builder()
            .claimType(CounterClaimType.PAYMENT_OR_COMPENSATION)
            .hwfReferenceNumber("HWF-123-456")
            .build();

        List<ListValue<UploadedDocument>> counterClaimDocuments = ListValueUtils.wrapListItems(List.of(mock(
            UploadedDocument.class)));

        DefendantResponses defendantResponses = DefendantResponses.builder()
            .counterClaim(counterClaim)
            .counterClaimDocuments(counterClaimDocuments)
            .build();
        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(defendantResponses)
            .build();
        CounterClaimEntity savedCounterClaim = mock(CounterClaimEntity.class);

        when(counterClaimService.saveCounterClaim(CASE_REFERENCE, counterClaim, partyEntity))
            .thenReturn(Optional.of(savedCounterClaim));
        when(counterClaimFeeCalculator.isHwfReferencePresent(counterClaim)).thenReturn(true);

        FeeDetails feeDetails = FeeDetails.builder().build();
        when(counterClaimFeeCalculator.getFeeDetails(counterClaim)).thenReturn(feeDetails);

        String expectedDescription = "some description";
        when(taskDescriptionService
                 .createReviewResponseAndCounterClaimDescription(CASE_REFERENCE, savedCounterClaim, feeDetails))
            .thenReturn(expectedDescription);

        // When
        RespondPossessionClaimSubmitPersistenceResult result =
            underTest.persistFinalSubmit(CASE_REFERENCE, possessionClaimResponse, partyEntity, journeyType);

        // Then
        assertThat(result.counterClaimEntity()).isEqualTo(savedCounterClaim);
        assertThat(result.paymentRequired()).isFalse();
        assertThat(result.feeDetails()).isEqualTo(feeDetails);
        verify(camundaService)
            .createTask(CASE_REFERENCE, TaskType.REVIEW_DEFENDANT_RESPONSE_AND_COUNTERCLAIM, expectedDescription);
    }

    @Test
    void shouldCreateCounterClaimTranslationTaskWhenHelpWithFeesAppliesAndWelshDocumentsExist() {
        JourneyType journeyType = JourneyType.CITIZEN;

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .build();
        when(partyEntity.getPcsCase()).thenReturn(pcsCaseEntity);

        CounterClaim counterClaim = CounterClaim.builder()
            .claimType(CounterClaimType.PAYMENT_OR_COMPENSATION)
            .hwfReferenceNumber("HWF-123-456")
            .build();
        DefendantResponses defendantResponses = DefendantResponses.builder()
            .counterClaim(counterClaim)
            .build();
        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(defendantResponses)
            .build();

        CounterClaimEntity savedCounterClaim = CounterClaimEntity.builder()
            .id(UUID.randomUUID())
            .party(partyEntity)
            .pcsCase(pcsCaseEntity)
            .status(CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED)
            .build();

        DefendantResponseEntity savedResponse = DefendantResponseEntity.builder()
            .id(1)
            .languageUsed(LanguageUsed.WELSH)
            .build();

        DocumentEntity counterClaimDocument = DocumentEntity.builder()
            .fileName("counterclaim-evidence.pdf")
            .counterClaim(savedCounterClaim)
            .build();

        when(defendantResponseService.saveDefendantResponse(
            CASE_REFERENCE, possessionClaimResponse, partyEntity, journeyType))
            .thenReturn(savedResponse);
        when(counterClaimService.saveCounterClaim(CASE_REFERENCE, counterClaim, partyEntity))
            .thenReturn(Optional.of(savedCounterClaim));
        when(documentService.createCounterClaimUploadedDocuments(
            defendantResponses.getCounterClaimDocuments(), savedCounterClaim, pcsCaseEntity, partyEntity))
            .thenReturn(List.of(counterClaimDocument));
        when(counterClaimFeeCalculator.isHwfReferencePresent(counterClaim)).thenReturn(true);
        when(translationWAService.isTranslationRequired(LanguageUsed.WELSH)).thenReturn(true);

        underTest.persistFinalSubmit(CASE_REFERENCE, possessionClaimResponse, partyEntity, journeyType);

        assertThat(savedCounterClaim.getStatus()).isEqualTo(CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED);
        verify(translationWAService).createTranslateDefendantSubmittedDocumentTask(
            pcsCaseEntity, partyEntity, List.of(counterClaimDocument));
    }

    @Test
    void shouldSaveCounterClaimDocumentsWhenPresent() {
        JourneyType journeyType = JourneyType.CITIZEN;

        CounterClaim counterClaim = CounterClaim.builder()
            .claimType(CounterClaimType.SOMETHING_ELSE)
            .build();
        UploadedDocument uploadedDocument = UploadedDocument.builder()
            .document(Document.builder().filename("evidence.pdf").build())
            .build();
        List<ListValue<UploadedDocument>> counterClaimDocuments = List.of(
            ListValue.<UploadedDocument>builder().id("doc-1").value(uploadedDocument).build()
        );
        DefendantResponses defendantResponses = DefendantResponses.builder()
            .counterClaim(counterClaim)
            .counterClaimDocuments(counterClaimDocuments)
            .build();
        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(defendantResponses)
            .build();
        PartyEntity partyEntity = PartyEntity.builder().id(UUID.randomUUID()).build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();
        CounterClaimEntity savedCounterClaim = CounterClaimEntity.builder()
            .id(UUID.randomUUID())
            .party(partyEntity)
            .pcsCase(pcsCaseEntity)
            .status(CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED)
            .build();

        when(counterClaimService.saveCounterClaim(CASE_REFERENCE, counterClaim, partyEntity))
            .thenReturn(Optional.of(savedCounterClaim));
        when(counterClaimFeeCalculator.isHwfReferencePresent(counterClaim)).thenReturn(false);

        underTest.persistFinalSubmit(CASE_REFERENCE, possessionClaimResponse, partyEntity, journeyType);

        verify(documentService).createCounterClaimUploadedDocuments(
            defendantResponses.getCounterClaimDocuments(),
            savedCounterClaim,
            savedCounterClaim.getPcsCase(),
            savedCounterClaim.getParty()
        );
    }

    @ParameterizedTest
    @EnumSource(JourneyType.class)
    void shouldScheduleCounterclaimEmailWhenCounterclaimPresent(JourneyType journeyType) {
        // Given
        CounterClaim counterClaim = CounterClaim.builder()
            .claimType(CounterClaimType.SOMETHING_ELSE)
            .build();

        DefendantResponses defendantResponses = DefendantResponses.builder()
            .counterClaim(counterClaim)
            .build();

        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(defendantResponses)
            .build();

        UUID savedCounterClaimId = UUID.randomUUID();
        CounterClaimEntity savedCounterClaim = CounterClaimEntity.builder()
            .id(savedCounterClaimId)
            .status(CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED)
            .build();

        when(counterClaimService.saveCounterClaim(CASE_REFERENCE, counterClaim, partyEntity))
            .thenReturn(Optional.of(savedCounterClaim));

        // When
        underTest.persistFinalSubmit(CASE_REFERENCE, possessionClaimResponse, partyEntity, journeyType);

        // Then
        verify(schedulerClient).scheduleIfNotExists(schedulableInstanceCaptor.capture());
        SchedulableInstance<CounterClaimTaskData> schedulableInstance = schedulableInstanceCaptor.getValue();

        TaskInstance<CounterClaimTaskData> taskInstance = schedulableInstance.getTaskInstance();
        assertThat(taskInstance.getTaskName()).isEqualTo(PENDING_COUNTER_CLAIM_ISSUED_TASK_DESCRIPTOR.getTaskName());
        assertThat(taskInstance.getData().getCounterClaimId()).isEqualTo(savedCounterClaimId);
    }

    @ParameterizedTest
    @EnumSource(JourneyType.class)
    void shouldNotScheduleCounterclaimEmailWhenCounterclaimNotPresent(JourneyType journeyType) {
        // Given
        DefendantResponses defendantResponses = DefendantResponses.builder()
            .counterClaim(null)
            .build();

        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(defendantResponses)
            .build();

        // When
        underTest.persistFinalSubmit(CASE_REFERENCE, possessionClaimResponse, partyEntity, journeyType);

        // Then
        verify(schedulerClient, never()).scheduleIfNotExists(any());
    }

    @Test
    void shouldNotCreateCounterClaimTranslationTaskWhenLanguageIsEnglish() {
        JourneyType journeyType = JourneyType.CITIZEN;

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .build();

        CounterClaim counterClaim = CounterClaim.builder()
            .claimType(CounterClaimType.PAYMENT_OR_COMPENSATION)
            .hwfReferenceNumber("HWF-123-456")
            .build();
        DefendantResponses defendantResponses = DefendantResponses.builder()
            .counterClaim(counterClaim)
            .build();
        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(defendantResponses)
            .build();

        CounterClaimEntity savedCounterClaim = CounterClaimEntity.builder()
            .id(UUID.randomUUID())
            .party(partyEntity)
            .pcsCase(pcsCaseEntity)
            .status(CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED)
            .build();

        DefendantResponseEntity savedResponse = DefendantResponseEntity.builder()
            .id(1)
            .languageUsed(LanguageUsed.ENGLISH)
            .build();

        DocumentEntity counterClaimDocument = DocumentEntity.builder()
            .fileName("counterclaim-evidence.pdf")
            .counterClaim(savedCounterClaim)
            .build();

        when(defendantResponseService.saveDefendantResponse(
            CASE_REFERENCE, possessionClaimResponse, partyEntity, journeyType))
            .thenReturn(savedResponse);
        when(counterClaimService.saveCounterClaim(CASE_REFERENCE, counterClaim, partyEntity))
            .thenReturn(Optional.of(savedCounterClaim));
        when(documentService.createCounterClaimUploadedDocuments(
            defendantResponses.getCounterClaimDocuments(), savedCounterClaim, pcsCaseEntity, partyEntity))
            .thenReturn(List.of(counterClaimDocument));
        when(counterClaimFeeCalculator.isHwfReferencePresent(counterClaim)).thenReturn(true);

        underTest.persistFinalSubmit(CASE_REFERENCE, possessionClaimResponse, partyEntity, journeyType);

        verify(translationWAService, never())
            .createTranslateDefendantSubmittedDocumentTask(any(), any(), any());
    }


}
