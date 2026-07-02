package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.sendletter.api.SendLetterApi;
import uk.gov.hmcts.reform.sendletter.api.SendLetterResponse;
import uk.gov.hmcts.reform.sendletter.api.model.v3.Document;
import uk.gov.hmcts.reform.sendletter.api.model.v3.LetterV3;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BulkPrintServiceTest {

    private static final UUID LETTER_ID = UUID.randomUUID();

    @Mock
    private CoversheetProvider coversheetProvider;
    @Mock
    private LetterDocumentFetcher letterDocumentFetcher;
    @Mock
    private SendLetterApi sendLetterApi;
    @Mock
    private AuthTokenGenerator authTokenGenerator;
    @Mock
    private CaseReferenceFormatter caseReferenceFormatter;

    @InjectMocks
    private BulkPrintService underTest;

    @Captor
    private ArgumentCaptor<LetterV3> letterCaptor;

    private final PcsCaseEntity pcsCase = PcsCaseEntity.builder().caseReference(1234567890123456L).build();
    private final PartyEntity recipient = PartyEntity.builder().id(UUID.randomUUID()).build();

    @BeforeEach
    void setUp() {
        when(authTokenGenerator.generate()).thenReturn("s2s");
        when(caseReferenceFormatter.formatCaseReferenceWithDashes(any())).thenReturn("1234-5678-9012-3456");
        when(coversheetProvider.render(any(), any(), any())).thenReturn(new Document("coversheet", 1));
        when(letterDocumentFetcher.fetch(any())).thenReturn(new Document("doc", 1));
        when(sendLetterApi.sendLetter(any(), any(LetterV3.class))).thenReturn(new SendLetterResponse(LETTER_ID));
    }

    @Test
    void assemblesCoversheetPlusDocumentsAndReturnsLetterId() {
        DocumentEntity claimForm = DocumentEntity.builder().documentId(UUID.randomUUID()).build();
        AddressUK address = AddressUK.builder()
            .addressLine1("1 High Street").postTown("London").postCode("W1 1AA").build();

        UUID letterId = underTest.sendPack(
            pcsCase, recipient, LetterType.CLAIMANT_CLAIM_PACK, "Jane Doe", address, List.of(claimForm));

        assertThat(letterId).isEqualTo(LETTER_ID);
        verify(coversheetProvider).render("Jane Doe", address, "1234-5678-9012-3456");
        verify(letterDocumentFetcher).fetch(claimForm.getDocumentId());
        verify(sendLetterApi).sendLetter(eq("s2s"), letterCaptor.capture());
        LetterV3 letter = letterCaptor.getValue();
        assertThat(letter.type).isEqualTo("CPC-01-IN1_pcs_api");
        assertThat(letter.documents).hasSize(2);            // coversheet + claim form
        assertThat(letter.additionalData)
            .containsEntry("caseReference", "1234-5678-9012-3456").containsKey("recipients");
    }

    @Test
    void throwsAndSendsNothingWhenNoPostalAddress() {
        AddressUK blank = AddressUK.builder().build();

        assertThatThrownBy(() -> underTest.sendPack(
            pcsCase, recipient, LetterType.DEFENDANT_CLAIM_PACK, "Jane Doe", blank, List.of()))
            .isInstanceOf(MissingPostalAddressException.class);

        verifyNoInteractions(coversheetProvider, letterDocumentFetcher, sendLetterApi);
    }
}
