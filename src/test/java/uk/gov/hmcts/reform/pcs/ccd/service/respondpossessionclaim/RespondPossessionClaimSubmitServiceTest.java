package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.UploadedDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaim;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimType;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

    private RespondPossessionClaimSubmitService underTest;

    @BeforeEach
    void setUp() {
        underTest = new RespondPossessionClaimSubmitService(
            claimResponseService,
            defendantResponseService,
            counterClaimService,
            counterClaimFeeCalculator,
            documentService,
            draftCaseDataService
        );
    }

    @Test
    void shouldPersistResponseWithoutCounterClaim() {
        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(DefendantResponses.builder().build())
            .build();

        when(counterClaimService.saveCounterClaim(CASE_REFERENCE, null)).thenReturn(Optional.empty());

        RespondPossessionClaimSubmitPersistenceResult result =
            underTest.persistFinalSubmit(CASE_REFERENCE, possessionClaimResponse);

        verify(claimResponseService).saveDraftData(possessionClaimResponse, CASE_REFERENCE);
        verify(defendantResponseService).saveDefendantResponse(CASE_REFERENCE, possessionClaimResponse);
        verify(draftCaseDataService).deleteUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim);
        verify(documentService, never()).createCounterClaimUploadedDocuments(any(), any(), any(), any());
        verify(counterClaimService, never()).issueCounterClaim(any());
        assertThat(result.counterClaimEntity()).isNull();
        assertThat(result.issuedWithoutPayment()).isFalse();
        assertThat(result.possessionClaimResponse()).isEqualTo(possessionClaimResponse);
    }

    @Test
    void shouldPersistCounterClaimAndCreatePaymentWhenFeeIsRequired() {
        CounterClaim counterClaim = CounterClaim.builder()
            .claimType(CounterClaimType.PAYMENT_OR_COMPENSATION)
            .isClaimAmountKnown(VerticalYesNo.YES)
            .claimAmount(new BigDecimal("250000"))
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

        when(counterClaimService.saveCounterClaim(CASE_REFERENCE, counterClaim))
            .thenReturn(Optional.of(savedCounterClaim));
        when(counterClaimFeeCalculator.isPaymentRequired(counterClaim)).thenReturn(true);

        RespondPossessionClaimSubmitPersistenceResult result =
            underTest.persistFinalSubmit(CASE_REFERENCE, possessionClaimResponse);

        verify(counterClaimService, never()).issueCounterClaim(any());
        assertThat(result.counterClaimEntity()).isEqualTo(savedCounterClaim);
        assertThat(result.issuedWithoutPayment()).isFalse();
    }

    @Test
    void shouldIssueCounterClaimImmediatelyWhenHelpWithFeesApplies() {
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
        CounterClaimEntity issuedCounterClaim = CounterClaimEntity.builder()
            .id(savedCounterClaim.getId())
            .status(CounterClaimState.COUNTER_CLAIM_ISSUED)
            .build();

        when(counterClaimService.saveCounterClaim(CASE_REFERENCE, counterClaim))
            .thenReturn(Optional.of(savedCounterClaim));
        when(counterClaimFeeCalculator.isPaymentRequired(counterClaim)).thenReturn(false);
        when(counterClaimService.issueCounterClaim(savedCounterClaim)).thenReturn(issuedCounterClaim);

        RespondPossessionClaimSubmitPersistenceResult result =
            underTest.persistFinalSubmit(CASE_REFERENCE, possessionClaimResponse);

        verify(counterClaimService).issueCounterClaim(savedCounterClaim);
        assertThat(result.counterClaimEntity()).isEqualTo(issuedCounterClaim);
        assertThat(result.issuedWithoutPayment()).isTrue();
    }

    @Test
    void shouldSaveCounterClaimDocumentsWhenPresent() {
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

        when(counterClaimService.saveCounterClaim(CASE_REFERENCE, counterClaim))
            .thenReturn(Optional.of(savedCounterClaim));
        when(counterClaimFeeCalculator.isPaymentRequired(counterClaim)).thenReturn(true);

        underTest.persistFinalSubmit(CASE_REFERENCE, possessionClaimResponse);

        verify(documentService).createCounterClaimUploadedDocuments(
            counterClaimDocuments,
            savedCounterClaim,
            pcsCaseEntity,
            partyEntity
        );
    }

    @Test
    void shouldSkipDocumentSaveWhenCounterClaimDocumentsAreEmpty() {
        CounterClaim counterClaim = CounterClaim.builder()
            .claimType(CounterClaimType.SOMETHING_ELSE)
            .build();
        DefendantResponses defendantResponses = DefendantResponses.builder()
            .counterClaim(counterClaim)
            .counterClaimDocuments(List.of())
            .build();
        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(defendantResponses)
            .build();
        CounterClaimEntity savedCounterClaim = CounterClaimEntity.builder()
            .id(UUID.randomUUID())
            .status(CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED)
            .build();

        when(counterClaimService.saveCounterClaim(CASE_REFERENCE, counterClaim))
            .thenReturn(Optional.of(savedCounterClaim));
        when(counterClaimFeeCalculator.isPaymentRequired(counterClaim)).thenReturn(true);

        underTest.persistFinalSubmit(CASE_REFERENCE, possessionClaimResponse);

        verify(documentService, never()).createCounterClaimUploadedDocuments(any(), any(), any(), any());
    }
}
