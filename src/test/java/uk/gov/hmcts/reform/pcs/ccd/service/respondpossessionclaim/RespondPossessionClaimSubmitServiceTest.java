package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.UploadedDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaim;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimType;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PreIssueChecklistEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.preissuechecklist.PreIssueChecklistService;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentService;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TranslationWAService;
import uk.gov.hmcts.reform.pcs.model.JourneyType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;

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
    private PreIssueChecklistService preIssueChecklistService;
    @Mock
    private PartyEntity partyEntity;

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
            translationWAService,
            preIssueChecklistService
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

        verify(claimResponseService).saveDraftDataForParty(possessionClaimResponse, partyEntity);
        verify(defendantResponseService)
            .saveDefendantResponse(CASE_REFERENCE, possessionClaimResponse, partyEntity, journeyType);
        verify(documentService, never()).createCounterClaimUploadedDocuments(any(), any(), any(), any());
        verify(counterClaimService, never()).issueCounterClaim(any());
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
        when(counterClaimFeeCalculator.isPaymentRequired(counterClaim)).thenReturn(true);

        RespondPossessionClaimSubmitPersistenceResult result =
            underTest.persistFinalSubmit(CASE_REFERENCE, possessionClaimResponse, partyEntity, journeyType);

        verify(counterClaimService, never()).issueCounterClaim(any());
        verify(draftCaseDataService).deleteUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim);
        assertThat(result.counterClaimEntity()).isEqualTo(savedCounterClaim);
        assertThat(result.paymentRequired()).isTrue();
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
        when(counterClaimFeeCalculator.isPaymentRequired(counterClaim)).thenReturn(true);

        RespondPossessionClaimSubmitPersistenceResult result =
            underTest.persistFinalSubmit(CASE_REFERENCE, possessionClaimResponse, partyEntity, journeyType);

        verify(counterClaimService, never()).issueCounterClaim(any());
        verify(draftCaseDataService).deleteUnsubmittedCaseData(
            CASE_REFERENCE, respondPossessionClaim, partyEntity.getId());
        assertThat(result.counterClaimEntity()).isEqualTo(savedCounterClaim);
        assertThat(result.paymentRequired()).isTrue();
    }

    @Test
    void shouldMoveCounterClaimToAwaitingCaseworkerReviewWhenHelpWithFeesApplies() {
        JourneyType journeyType = JourneyType.CITIZEN;

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
            .status(CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED)
            .build();

        when(counterClaimService.saveCounterClaim(CASE_REFERENCE, counterClaim, partyEntity))
            .thenReturn(Optional.of(savedCounterClaim));
        when(counterClaimFeeCalculator.isPaymentRequired(counterClaim)).thenReturn(false);

        RespondPossessionClaimSubmitPersistenceResult result =
            underTest.persistFinalSubmit(CASE_REFERENCE, possessionClaimResponse, partyEntity, journeyType);

        verify(counterClaimService, never()).issueCounterClaim(any());
        assertThat(result.counterClaimEntity()).isEqualTo(savedCounterClaim);
        assertThat(result.counterClaimEntity().getStatus())
            .isEqualTo(CounterClaimState.PENDING_REVIEW);
        assertThat(result.paymentRequired()).isFalse();
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
        pcsCaseEntity.setDocuments(List.of(counterClaimDocument));

        when(defendantResponseService.saveDefendantResponse(
            CASE_REFERENCE, possessionClaimResponse, partyEntity, journeyType))
            .thenReturn(savedResponse);
        when(counterClaimService.saveCounterClaim(CASE_REFERENCE, counterClaim, partyEntity))
            .thenReturn(Optional.of(savedCounterClaim));
        when(counterClaimFeeCalculator.isPaymentRequired(counterClaim)).thenReturn(false);

        underTest.persistFinalSubmit(CASE_REFERENCE, possessionClaimResponse, partyEntity, journeyType);

        assertThat(savedCounterClaim.getStatus()).isEqualTo(CounterClaimState.PENDING_REVIEW);
        verify(preIssueChecklistService).save(any(PreIssueChecklistEntity.class));
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
        when(counterClaimFeeCalculator.isPaymentRequired(counterClaim)).thenReturn(true);

        underTest.persistFinalSubmit(CASE_REFERENCE, possessionClaimResponse, partyEntity, journeyType);

        verify(documentService).createCounterClaimUploadedDocuments(
            defendantResponses.getCounterClaimDocuments(),
            savedCounterClaim,
            savedCounterClaim.getPcsCase(),
            savedCounterClaim.getParty()
        );
    }

    @Test
    void shouldNotCreateTranslateTaskWhenResponseLanguageIsEnglish() {
        JourneyType journeyType = JourneyType.CITIZEN;

        PartyEntity partyEntity = PartyEntity.builder().id(UUID.randomUUID()).build();
        DefendantResponseEntity savedResponse = DefendantResponseEntity.builder()
            .id(1)
            .languageUsed(LanguageUsed.ENGLISH)
            .build();

        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(DefendantResponses.builder().build())
            .build();

        when(defendantResponseService.saveDefendantResponse(
            CASE_REFERENCE, possessionClaimResponse, partyEntity, journeyType))
            .thenReturn(savedResponse);
        when(counterClaimService.saveCounterClaim(CASE_REFERENCE, null, partyEntity)).thenReturn(Optional.empty());

        underTest.persistFinalSubmit(CASE_REFERENCE, possessionClaimResponse, partyEntity, journeyType);

        verifyNoInteractions(translationWAService);
    }

    @Test
    void shouldCreateTranslateTaskWhenLanguageIsEnglishAndWelsh() {
        JourneyType journeyType = JourneyType.CITIZEN;

        ClaimEntity mainClaim = ClaimEntity.builder().id(UUID.randomUUID()).build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .claims(List.of(mainClaim))
            .build();
        PartyEntity partyEntity = PartyEntity.builder().id(UUID.randomUUID()).pcsCase(pcsCaseEntity).build();

        DefendantResponseEntity savedResponse = DefendantResponseEntity.builder()
            .id(1)
            .languageUsed(LanguageUsed.ENGLISH_AND_WELSH)
            .build();
        DefendantResponseEntity otherResponse = DefendantResponseEntity.builder().id(2).build();

        DocumentEntity activeDocument = DocumentEntity.builder()
            .fileName("evidence.pdf")
            .defendantResponse(savedResponse)
            .build();
        DocumentEntity removedDocument = DocumentEntity.builder()
            .defendantResponse(savedResponse)
            .removed(true)
            .build();
        DocumentEntity unrelatedResponseDocument = DocumentEntity.builder()
            .defendantResponse(otherResponse)
            .build();
        DocumentEntity noResponseDocument = DocumentEntity.builder().build();
        pcsCaseEntity.setDocuments(
            List.of(activeDocument, removedDocument, unrelatedResponseDocument, noResponseDocument));

        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(DefendantResponses.builder().build())
            .build();

        when(defendantResponseService.saveDefendantResponse(
            CASE_REFERENCE, possessionClaimResponse, partyEntity, journeyType))
            .thenReturn(savedResponse);
        when(counterClaimService.saveCounterClaim(CASE_REFERENCE, null, partyEntity)).thenReturn(Optional.empty());

        underTest.persistFinalSubmit(CASE_REFERENCE, possessionClaimResponse, partyEntity, journeyType);

        verify(translationWAService).createTranslateDefendantSubmittedDocumentTask(
            pcsCaseEntity, partyEntity, List.of(activeDocument));
        verify(preIssueChecklistService).save(any(PreIssueChecklistEntity.class));
    }

    @Test
    void shouldExcludeCounterClaimDocumentsFromTranslationTask() {
        JourneyType journeyType = JourneyType.CITIZEN;

        ClaimEntity mainClaim = ClaimEntity.builder().id(UUID.randomUUID()).build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .claims(List.of(mainClaim))
            .build();
        PartyEntity partyEntity = PartyEntity.builder().id(UUID.randomUUID()).pcsCase(pcsCaseEntity).build();

        DefendantResponseEntity savedResponse = DefendantResponseEntity.builder()
            .id(1)
            .languageUsed(LanguageUsed.WELSH)
            .build();
        DocumentEntity responseDocument = DocumentEntity.builder()
            .fileName("response-evidence.pdf")
            .defendantResponse(savedResponse)
            .build();
        pcsCaseEntity.setDocuments(List.of(responseDocument));

        CounterClaim counterClaim = CounterClaim.builder().claimType(CounterClaimType.SOMETHING_ELSE).build();
        DefendantResponses defendantResponses = DefendantResponses.builder().counterClaim(counterClaim).build();
        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(defendantResponses)
            .build();

        CounterClaimEntity savedCounterClaim = CounterClaimEntity.builder()
            .id(UUID.randomUUID())
            .party(partyEntity)
            .pcsCase(pcsCaseEntity)
            .status(CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED)
            .build();

        when(defendantResponseService.saveDefendantResponse(
            CASE_REFERENCE, possessionClaimResponse, partyEntity, journeyType))
            .thenReturn(savedResponse);
        when(counterClaimService.saveCounterClaim(CASE_REFERENCE, counterClaim, partyEntity))
            .thenReturn(Optional.of(savedCounterClaim));

        underTest.persistFinalSubmit(CASE_REFERENCE, possessionClaimResponse, partyEntity, journeyType);

        verify(translationWAService).createTranslateDefendantSubmittedDocumentTask(
            pcsCaseEntity, partyEntity, List.of(responseDocument));
    }

    @Test
    void shouldNotCreateTranslateTaskWhenWelshButNoDocumentsExist() {
        JourneyType journeyType = JourneyType.CITIZEN;

        ClaimEntity mainClaim = ClaimEntity.builder().id(UUID.randomUUID()).build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .claims(List.of(mainClaim))
            .build();
        PartyEntity partyEntity = PartyEntity.builder().id(UUID.randomUUID()).pcsCase(pcsCaseEntity).build();

        DefendantResponseEntity savedResponse = DefendantResponseEntity.builder()
            .id(1)
            .languageUsed(LanguageUsed.WELSH)
            .build();

        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(DefendantResponses.builder().build())
            .build();

        when(defendantResponseService.saveDefendantResponse(
            CASE_REFERENCE, possessionClaimResponse, partyEntity, journeyType))
            .thenReturn(savedResponse);
        when(counterClaimService.saveCounterClaim(CASE_REFERENCE, null, partyEntity)).thenReturn(Optional.empty());

        underTest.persistFinalSubmit(CASE_REFERENCE, possessionClaimResponse, partyEntity, journeyType);

        verify(translationWAService)
            .createTranslateDefendantSubmittedDocumentTask(pcsCaseEntity, partyEntity, List.of());
        verifyNoInteractions(preIssueChecklistService);
    }

}
