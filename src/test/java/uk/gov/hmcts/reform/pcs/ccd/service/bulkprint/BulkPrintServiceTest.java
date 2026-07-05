package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.service.coversheet.CoversheetProvider;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.sendletter.api.SendLetterApi;
import uk.gov.hmcts.reform.sendletter.api.SendLetterResponse;
import uk.gov.hmcts.reform.sendletter.api.model.v3.LetterV3;

import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BulkPrintServiceTest {

    private static final UUID LETTER_ID = UUID.randomUUID();

    @Mock
    private CoversheetProvider coversheetProvider;
    @Mock
    private LetterDocumentFetcher letterDocumentFetcher;
    @Mock
    private PdfMerger pdfMerger;
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

    @Test
    @DisplayName("Merges the coversheet and documents into one letter and returns the letter id")
    void shouldMergeCoversheetPlusDocumentsAndReturnLetterId() {
        byte[] coversheetBytes = "coversheet".getBytes();
        byte[] claimFormBytes = "claim-form".getBytes();
        byte[] mergedBytes = "merged-pdf".getBytes();
        when(authTokenGenerator.generate()).thenReturn("s2s");
        when(caseReferenceFormatter.formatCaseReferenceWithDashes(any())).thenReturn("1234-5678-9012-3456");
        when(coversheetProvider.render(any(), any(), any())).thenReturn(coversheetBytes);
        when(letterDocumentFetcher.fetchBytes(any())).thenReturn(claimFormBytes);
        when(pdfMerger.merge(List.of(coversheetBytes, claimFormBytes))).thenReturn(mergedBytes);
        when(sendLetterApi.sendLetter(any(), any(LetterV3.class))).thenReturn(new SendLetterResponse(LETTER_ID));

        DocumentEntity claimForm = DocumentEntity.builder().id(UUID.randomUUID()).documentId(UUID.randomUUID()).build();
        AddressUK address = AddressUK.builder()
            .addressLine1("1 High Street").postTown("London").postCode("W1 1AA").build();

        UUID letterId = underTest.sendPack(
            pcsCase, recipient, LetterType.CLAIMANT_CLAIM_PACK, "Jane Doe", address, List.of(claimForm));

        assertThat(letterId).isEqualTo(LETTER_ID);
        verify(coversheetProvider).render("Jane Doe", address, "1234-5678-9012-3456");
        verify(letterDocumentFetcher).fetchBytes(claimForm.getDocumentId());
        verify(sendLetterApi).sendLetter(eq("s2s"), letterCaptor.capture());
        LetterV3 letter = letterCaptor.getValue();
        assertThat(letter.type).isEqualTo("CPC-01-IN1_pcs_api");
        assertThat(letter.documents).singleElement()
            .extracting(document -> document.content)
            .isEqualTo(Base64.getEncoder().encodeToString(mergedBytes));
        assertThat(letter.additionalData).containsEntry("caseReference", "1234-5678-9012-3456");
        assertThat(letter.additionalData.get("recipients"))
            .asInstanceOf(list(String.class))
            .containsExactly("Jane Doe", "1 High Street", "London", "W1 1AA");
    }

    @Test
    @DisplayName("Throws and sends nothing when the postal address is missing")
    void shouldThrowAndSendNothingWhenPostalAddressMissing() {
        AddressUK blank = AddressUK.builder().build();

        assertThatThrownBy(() -> underTest.sendPack(
            pcsCase, recipient, LetterType.DEFENDANT_CLAIM_PACK, "Jane Doe", blank, List.of()))
            .isInstanceOf(MissingPostalAddressException.class);

        verifyNoInteractions(coversheetProvider, letterDocumentFetcher, pdfMerger, sendLetterApi);
    }
}
