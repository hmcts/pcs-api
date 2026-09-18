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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenAppPackSenderTest {

    private static final UUID CASE_ID = UUID.randomUUID();

    @Mock
    private PackRecipientResolver packRecipientResolver;
    @Mock
    private BulkPrintService bulkPrintService;
    @Mock
    private AccessCodeActivityLogService accessCodeActivityLogService;

    private GenAppPackSender underTest;

    private final PcsCaseEntity pcsCase = PcsCaseEntity.builder().caseReference(1234567890123456L).build();
    private final PartyEntity recipient = PartyEntity.builder().id(UUID.randomUUID()).build();
    private final DocumentEntity gaForm = DocumentEntity.builder().id(UUID.randomUUID()).build();
    private final AddressUK address = AddressUK.builder().addressLine1("1 High Street").build();

    @BeforeEach
    void setUp() {
        underTest = new GenAppPackSender(packRecipientResolver, bulkPrintService,
            new PackSendRecorder(accessCodeActivityLogService));
    }

    @Test
    @DisplayName("Does nothing when there are no recipients")
    void shouldDoNothingWhenNoRecipients() {
        when(packRecipientResolver.resolveGenAppRecipients(CASE_ID)).thenReturn(List.of());

        underTest.sendGenAppPacks(CASE_ID);

        verifyNoInteractions(bulkPrintService, accessCodeActivityLogService);
    }

    @Test
    @DisplayName("Posts each resolved recipient with GEN_APP_PACK and records the send")
    void shouldPostRecipientAndRecordPackSent() {
        when(packRecipientResolver.resolveGenAppRecipients(CASE_ID)).thenReturn(List.of(
            new ResolvedRecipient(pcsCase, recipient, LetterType.GEN_APP_PACK,
                List.of(gaForm), "Acme Ltd", address)));
        when(bulkPrintService.sendPack(any(), any(), any(), any(), any(), any())).thenReturn(UUID.randomUUID());

        underTest.sendGenAppPacks(CASE_ID);

        verify(bulkPrintService).sendPack(pcsCase, recipient, LetterType.GEN_APP_PACK, "Acme Ltd", address,
            List.of(gaForm));
        verify(accessCodeActivityLogService).recordPackSent(eq(pcsCase), eq(recipient), any(PackDetails.class));
    }

    @Test
    @DisplayName("Records a pack-send failure when the send throws a missing address")
    void shouldRecordFailureWhenSendThrowsMissingAddress() {
        when(packRecipientResolver.resolveGenAppRecipients(CASE_ID)).thenReturn(List.of(
            new ResolvedRecipient(pcsCase, recipient, LetterType.GEN_APP_PACK, List.of(gaForm),
                "Acme Ltd", address)));
        when(bulkPrintService.sendPack(any(), any(), any(), any(), any(), any()))
            .thenThrow(new MissingPostalAddressException("no address"));

        underTest.sendGenAppPacks(CASE_ID);

        verify(accessCodeActivityLogService).recordPackFailed(eq(pcsCase), eq(recipient), any(PackDetails.class));
    }
}
