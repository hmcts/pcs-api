package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.AccessCodeActivityLogService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefencePackSenderTest {

    private static final UUID CASE_ID = UUID.randomUUID();

    @Mock
    private PackRecipientResolver packRecipientResolver;
    @Mock
    private BulkPrintService bulkPrintService;
    @Mock
    private AccessCodeActivityLogService accessCodeActivityLogService;

    private DefencePackSender underTest;

    private final PcsCaseEntity pcsCase = PcsCaseEntity.builder().caseReference(1234567890123456L).build();
    private final PartyEntity defendant = PartyEntity.builder().id(UUID.randomUUID()).build();
    private final DocumentEntity defenceForm = DocumentEntity.builder().id(UUID.randomUUID()).build();
    private final DocumentEntity counterClaim = DocumentEntity.builder().id(UUID.randomUUID()).build();
    private final AddressUK address = AddressUK.builder().addressLine1("42 Renters Way").build();

    @BeforeEach
    void setUp() {
        underTest = new DefencePackSender(packRecipientResolver, bulkPrintService,
            new PackSendRecorder(accessCodeActivityLogService));
    }

    @Test
    @DisplayName("Does nothing when there are no recipients")
    void shouldDoNothingWhenNoRecipients() {
        when(packRecipientResolver.resolveDefenceRecipients(CASE_ID)).thenReturn(List.of());

        underTest.sendDefencePacks(CASE_ID);

        verifyNoInteractions(bulkPrintService, accessCodeActivityLogService);
    }

    @Test
    @DisplayName("Posts each resolved recipient and records every document sent")
    void shouldPostRecipientAndRecordEachDocument() {
        when(packRecipientResolver.resolveDefenceRecipients(CASE_ID)).thenReturn(List.of(
            new ResolvedRecipient(pcsCase, defendant, LetterType.DEFENCE_PACK,
                List.of(defenceForm, counterClaim), "Bob Tenant", address)));
        when(bulkPrintService.sendPack(any(), any(), any(), any(), any(), any())).thenReturn(UUID.randomUUID());

        underTest.sendDefencePacks(CASE_ID);

        verify(bulkPrintService).sendPack(pcsCase, defendant, LetterType.DEFENCE_PACK, "Bob Tenant", address,
            List.of(defenceForm, counterClaim));
        verify(accessCodeActivityLogService).recordDocumentSent(pcsCase, defendant, defenceForm);
        verify(accessCodeActivityLogService).recordDocumentSent(pcsCase, defendant, counterClaim);
    }

    @Test
    @DisplayName("Records a document-send failure when the send throws a missing address")
    void shouldRecordFailureWhenSendThrowsMissingAddress() {
        when(packRecipientResolver.resolveDefenceRecipients(CASE_ID)).thenReturn(List.of(
            new ResolvedRecipient(pcsCase, defendant, LetterType.DEFENCE_PACK, List.of(defenceForm),
                "Bob Tenant", address)));
        when(bulkPrintService.sendPack(any(), any(), any(), any(), any(), any()))
            .thenThrow(new MissingPostalAddressException("no address"));

        underTest.sendDefencePacks(CASE_ID);

        verify(accessCodeActivityLogService).recordDocumentSendFailure(pcsCase, defendant, defenceForm);
    }
}
