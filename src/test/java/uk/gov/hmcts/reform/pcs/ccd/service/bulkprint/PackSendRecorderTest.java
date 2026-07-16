package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.FailureReason;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.PackDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.AccessCodeActivityLogService;
import uk.gov.hmcts.reform.pcs.exception.RedactionContext;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.pcs.exception.ErrorCode.MISSING_POSTAL_ADDRESS;

@ExtendWith(MockitoExtension.class)
class PackSendRecorderTest {

    @Mock
    private AccessCodeActivityLogService accessCodeActivityLogService;

    @InjectMocks
    private PackSendRecorder underTest;

    @Captor
    private ArgumentCaptor<PackDetails> packDetailsCaptor;

    private final PcsCaseEntity pcsCase = PcsCaseEntity.builder().caseReference(1234567890123456L).build();
    private final PartyEntity recipient = PartyEntity.builder().id(UUID.randomUUID()).build();
    private final DocumentEntity document = DocumentEntity.builder()
        .id(UUID.randomUUID()).type(DocumentType.DEFENDANT_RESPONSE).build();

    @Test
    @DisplayName("Records a PACK_SENT row carrying the letterId and documents on success")
    void shouldRecordPackSentOnSuccess() {
        UUID letterId = UUID.randomUUID();
        Supplier<UUID> sendAction = () -> letterId;

        underTest.sendAndRecord(pcsCase, recipient, LetterType.DEFENCE_PACK, List.of(document), sendAction);

        verify(accessCodeActivityLogService)
            .recordPackSent(eq(pcsCase), eq(recipient), packDetailsCaptor.capture());
        verify(accessCodeActivityLogService, never()).recordPackFailed(any(), any(), any());
        PackDetails details = packDetailsCaptor.getValue();
        assertThat(details.packType()).isEqualTo(LetterType.DEFENCE_PACK);
        assertThat(details.letterId()).isEqualTo(letterId);
        assertThat(details.documents()).singleElement().satisfies(ref -> {
            assertThat(ref.id()).isEqualTo(document.getId());
            assertThat(ref.type()).isEqualTo(DocumentType.DEFENDANT_RESPONSE);
        });
    }

    @Test
    @DisplayName("Marks the claim form self=true for the claimant and self=false for a defendant")
    void shouldResolveClaimFormOwnershipToClaimant() {
        PartyEntity claimant = PartyEntity.builder().id(UUID.randomUUID()).build();
        PartyEntity defendant = PartyEntity.builder().id(UUID.randomUUID()).build();
        ClaimEntity claim = ClaimEntity.builder()
            .claimParties(List.of(
                ClaimPartyEntity.builder().party(claimant).role(PartyRole.CLAIMANT).build(),
                ClaimPartyEntity.builder().party(defendant).role(PartyRole.DEFENDANT).rank(1).build()))
            .build();
        PcsCaseEntity caseWithClaim = PcsCaseEntity.builder()
            .caseReference(1234567890123456L).claims(List.of(claim)).build();
        DocumentEntity claimForm = DocumentEntity.builder()
            .id(UUID.randomUUID()).type(DocumentType.CLAIM).build();
        DocumentEntity accessCode = DocumentEntity.builder()
            .id(UUID.randomUUID()).type(DocumentType.DEFENDANT_ACCESS_CODE).party(defendant).build();

        underTest.sendAndRecord(caseWithClaim, claimant, LetterType.CLAIMANT_CLAIM_PACK,
                                List.of(claimForm), () -> UUID.randomUUID());
        underTest.sendAndRecord(caseWithClaim, defendant, LetterType.DEFENDANT_CLAIM_PACK,
                                List.of(claimForm, accessCode), () -> UUID.randomUUID());

        verify(accessCodeActivityLogService)
            .recordPackSent(eq(caseWithClaim), eq(claimant), packDetailsCaptor.capture());
        assertThat(packDetailsCaptor.getValue().documents()).singleElement().satisfies(ref -> {
            assertThat(ref.self()).isTrue();
            assertThat(ref.defendantNumber()).isNull();
        });

        verify(accessCodeActivityLogService)
            .recordPackSent(eq(caseWithClaim), eq(defendant), packDetailsCaptor.capture());
        assertThat(packDetailsCaptor.getValue().documents()).satisfiesExactly(
            claimRef -> {
                assertThat(claimRef.type()).isEqualTo(DocumentType.CLAIM);
                assertThat(claimRef.self()).isFalse();
                assertThat(claimRef.defendantNumber()).isNull();
            },
            accessCodeRef -> {
                assertThat(accessCodeRef.type()).isEqualTo(DocumentType.DEFENDANT_ACCESS_CODE);
                assertThat(accessCodeRef.self()).isTrue();
                assertThat(accessCodeRef.defendantNumber()).isEqualTo(1);
            });
    }

    @Test
    @DisplayName("Records a retryable PACK_FAILED row on a generic send failure")
    void shouldRecordRetryableFailureOnGenericException() {
        Supplier<UUID> sendAction = () -> {
            throw new RuntimeException("send-letter blew up");
        };

        underTest.sendAndRecord(pcsCase, recipient, LetterType.DEFENCE_PACK, List.of(document), sendAction);

        verify(accessCodeActivityLogService)
            .recordPackFailed(eq(pcsCase), eq(recipient), packDetailsCaptor.capture());
        verify(accessCodeActivityLogService, never()).recordPackSent(any(), any(), any());
        PackDetails details = packDetailsCaptor.getValue();
        assertThat(details.terminal()).isFalse();
        assertThat(details.failureReason()).isEqualTo(FailureReason.UNKNOWN);
        assertThat(details.letterId()).isNull();
        assertThat(details.errorDetail()).isEqualTo("RuntimeException: send-letter blew up");
    }

    @Test
    @DisplayName("Records a terminal PACK_FAILED row when the recipient has no postal address")
    void shouldRecordTerminalFailureOnMissingAddress() {
        Supplier<UUID> sendAction = () -> {
            throw new MissingPostalAddressException(MISSING_POSTAL_ADDRESS, RedactionContext.empty());
        };

        underTest.sendAndRecord(pcsCase, recipient, LetterType.DEFENCE_PACK, List.of(document), sendAction);

        verify(accessCodeActivityLogService)
            .recordPackFailed(eq(pcsCase), eq(recipient), packDetailsCaptor.capture());
        verify(accessCodeActivityLogService, never()).recordPackSent(any(), any(), any());
        PackDetails details = packDetailsCaptor.getValue();
        assertThat(details.terminal()).isTrue();
        assertThat(details.failureReason()).isEqualTo(FailureReason.MISSING_ADDRESS);
        assertThat(details.errorDetail()).isEqualTo("MissingPostalAddressException: no usable address");
    }
}
