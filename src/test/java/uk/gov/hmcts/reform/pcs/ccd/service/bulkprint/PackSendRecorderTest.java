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
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.AccessCodeActivityLogService;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
    }

    @Test
    @DisplayName("Records a terminal PACK_FAILED row when the recipient has no postal address")
    void shouldRecordTerminalFailureOnMissingAddress() {
        Supplier<UUID> sendAction = () -> {
            throw new MissingPostalAddressException("no usable address");
        };

        underTest.sendAndRecord(pcsCase, recipient, LetterType.DEFENCE_PACK, List.of(document), sendAction);

        verify(accessCodeActivityLogService)
            .recordPackFailed(eq(pcsCase), eq(recipient), packDetailsCaptor.capture());
        verify(accessCodeActivityLogService, never()).recordPackSent(any(), any(), any());
        PackDetails details = packDetailsCaptor.getValue();
        assertThat(details.terminal()).isTrue();
        assertThat(details.failureReason()).isEqualTo(FailureReason.MISSING_ADDRESS);
    }
}
