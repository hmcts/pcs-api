package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.PackDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.AccessCodeActivityLogService;
import uk.gov.hmcts.reform.pcs.exception.RedactionContext;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.exception.ErrorCode.MISSING_POSTAL_ADDRESS;

@ExtendWith(MockitoExtension.class)
class ClaimPackSenderTest {

    private static final UUID CASE_ID = UUID.randomUUID();

    @Mock
    private PackRecipientResolver packRecipientResolver;
    @Mock
    private BulkPrintService bulkPrintService;
    @Mock
    private AccessCodeActivityLogService accessCodeActivityLogService;

    private ClaimPackSender underTest;

    private final PcsCaseEntity pcsCase = PcsCaseEntity.builder().caseReference(1234567890123456L).build();
    private final PartyEntity recipient = PartyEntity.builder().id(UUID.randomUUID()).build();
    private final DocumentEntity claimForm = DocumentEntity.builder().id(UUID.randomUUID()).build();
    private final AddressUK address = AddressUK.builder().addressLine1("1 High Street").build();

    @BeforeEach
    void setUp() {
        underTest = new ClaimPackSender(packRecipientResolver, bulkPrintService,
            new PackSendRecorder(accessCodeActivityLogService));
    }

    @Test
    @DisplayName("Does nothing when there are no recipients")
    void shouldDoNothingWhenNoRecipients() {
        when(packRecipientResolver.resolveClaimRecipients(CASE_ID)).thenReturn(List.of());

        underTest.sendClaimPacks(CASE_ID);

        verifyNoInteractions(bulkPrintService, accessCodeActivityLogService);
    }

    @Test
    @DisplayName("Posts each resolved recipient and records the document sent")
    void shouldPostResolvedRecipientAndRecordDocumentSent() {
        when(packRecipientResolver.resolveClaimRecipients(CASE_ID)).thenReturn(List.of(resolvedRecipient()));
        when(bulkPrintService.sendPack(any(), any(), any(), any(), any(), any())).thenReturn(UUID.randomUUID());

        underTest.sendClaimPacks(CASE_ID);

        verify(bulkPrintService).sendPack(pcsCase, recipient,
            LetterType.CLAIMANT_CLAIM_PACK, "Acme Ltd", address, List.of(claimForm));
        verify(accessCodeActivityLogService).recordPackSent(eq(pcsCase), eq(recipient), any(PackDetails.class));
        verify(accessCodeActivityLogService, never()).recordPackFailed(any(), any(), any());
    }

    @Test
    @DisplayName("Records a document-send failure when the send throws a missing address")
    void shouldRecordFailureWhenSendThrowsMissingAddress() {
        when(packRecipientResolver.resolveClaimRecipients(CASE_ID)).thenReturn(List.of(resolvedRecipient()));
        when(bulkPrintService.sendPack(any(), any(), any(), any(), any(), any()))
            .thenThrow(new MissingPostalAddressException(MISSING_POSTAL_ADDRESS, RedactionContext.empty()));

        underTest.sendClaimPacks(CASE_ID);

        verify(accessCodeActivityLogService).recordPackFailed(eq(pcsCase), eq(recipient), any(PackDetails.class));
    }

    private ResolvedRecipient resolvedRecipient() {
        return new ResolvedRecipient(pcsCase, recipient, LetterType.CLAIMANT_CLAIM_PACK, List.of(claimForm),
            "Acme Ltd", address);
    }
}
