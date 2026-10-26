package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.service.coverletter.CoveringLetterProvider;
import uk.gov.hmcts.reform.pcs.ccd.service.coversheet.CoversheetProvider;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BulkPrintServiceTest {

    private static final UUID LETTER_ID = UUID.randomUUID();

    @Mock
    private CoversheetProvider coversheetProvider;
    @Mock
    private CoveringLetterProvider coveringLetterProvider;
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
    @Mock
    private FeatureToggleService featureToggleService;

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
        assertThat(letter.documents).singleElement()
            .extracting(document -> document.content)
            .isEqualTo(Base64.getEncoder().encodeToString(mergedBytes));
        assertThat(letter.additionalData).containsEntry("caseReference", "1234-5678-9012-3456");
        assertThat(letter.additionalData.get("recipients"))
            .asInstanceOf(list(String.class))
            .containsExactly("Jane Doe", "1 High Street", "London", "W1 1AA");
    }

    @ParameterizedTest(name = "{0} sends letter type {1}")
    @CsvSource({
        "CLAIMANT_CLAIM_PACK, CPC-01-IN0",
        "DEFENCE_PACK, DEF-01-IN0",
        "DEFENCE_PACK_LEGAL_REP, DEF-01-IN1",
        "GEN_APP_PACK, GEN-01-IN0"
    })
    @DisplayName("Sends renamed IN0 letter type codes for claim and defence packs")
    void shouldSendRenamedLetterTypeCode(LetterType letterType, String expectedLetterType) {
        when(authTokenGenerator.generate()).thenReturn("s2s");
        when(caseReferenceFormatter.formatCaseReferenceWithDashes(any())).thenReturn("1234-5678-9012-3456");
        byte[] coversheetBytes = "coversheet".getBytes();
        when(coversheetProvider.render(any(), any(), any())).thenReturn(coversheetBytes);
        byte[] letterBytes = "covering-letter".getBytes();
        if (letterType.includesCoveringLetter()) {
            when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4)).thenReturn(true);
            when(coveringLetterProvider.render(any(), any(), any(), any(), any())).thenReturn(letterBytes);
        }
        byte[] documentBytes = "document".getBytes();
        when(letterDocumentFetcher.fetchBytes(any())).thenReturn(documentBytes);
        List<byte[]> pdfs = letterType.includesCoveringLetter()
            ? List.of(coversheetBytes, letterBytes, documentBytes)
            : List.of(coversheetBytes, documentBytes);
        byte[] mergedBytes = "merged-pdf".getBytes();
        when(pdfMerger.merge(pdfs)).thenReturn(mergedBytes);
        when(sendLetterApi.sendLetter(any(), any(LetterV3.class))).thenReturn(new SendLetterResponse(LETTER_ID));

        DocumentEntity document = DocumentEntity.builder().id(UUID.randomUUID()).documentId(UUID.randomUUID()).build();
        AddressUK address = AddressUK.builder()
            .addressLine1("1 High Street").postTown("London").postCode("W1 1AA").build();

        underTest.sendPack(pcsCase, recipient, letterType, "Jane Doe", address, List.of(document));

        verify(sendLetterApi).sendLetter(eq("s2s"), letterCaptor.capture());
        assertThat(letterCaptor.getValue().type).isEqualTo(expectedLetterType);
        verify(pdfMerger).merge(pdfs);
    }

    @Test
    @DisplayName("Posts a defence pack without the covering letter when release 1.4 is off")
    void shouldOmitCoveringLetterWhenRelease14IsOff() {
        when(authTokenGenerator.generate()).thenReturn("s2s");
        when(caseReferenceFormatter.formatCaseReferenceWithDashes(any())).thenReturn("1234-5678-9012-3456");
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4)).thenReturn(false);
        byte[] coversheetBytes = "coversheet".getBytes();
        when(coversheetProvider.render(any(), any(), any())).thenReturn(coversheetBytes);
        byte[] documentBytes = "document".getBytes();
        when(letterDocumentFetcher.fetchBytes(any())).thenReturn(documentBytes);
        byte[] mergedBytes = "merged-pdf".getBytes();
        when(pdfMerger.merge(List.of(coversheetBytes, documentBytes))).thenReturn(mergedBytes);
        when(sendLetterApi.sendLetter(any(), any(LetterV3.class))).thenReturn(new SendLetterResponse(LETTER_ID));

        DocumentEntity document = DocumentEntity.builder().id(UUID.randomUUID()).documentId(UUID.randomUUID()).build();
        AddressUK address = AddressUK.builder()
            .addressLine1("1 High Street").postTown("London").postCode("W1 1AA").build();

        underTest.sendPack(pcsCase, recipient, LetterType.DEFENCE_PACK, "Jane Doe", address, List.of(document));

        verify(pdfMerger).merge(List.of(coversheetBytes, documentBytes));
        verifyNoInteractions(coveringLetterProvider);
    }

    @Test
    @DisplayName("Numbers a later defendant's defence pack and general-application pack")
    void shouldNumberLetterCodesByRecipientSequence() {
        when(authTokenGenerator.generate()).thenReturn("s2s");
        when(caseReferenceFormatter.formatCaseReferenceWithDashes(any())).thenReturn("1234-5678-9012-3456");
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4)).thenReturn(false);
        byte[] coversheetBytes = "coversheet".getBytes();
        when(coversheetProvider.render(any(), any(), any())).thenReturn(coversheetBytes);
        byte[] documentBytes = "document".getBytes();
        when(letterDocumentFetcher.fetchBytes(any())).thenReturn(documentBytes);
        byte[] mergedBytes = "merged-pdf".getBytes();
        when(pdfMerger.merge(List.of(coversheetBytes, documentBytes))).thenReturn(mergedBytes);
        when(sendLetterApi.sendLetter(any(), any(LetterV3.class))).thenReturn(new SendLetterResponse(LETTER_ID));

        DocumentEntity document = DocumentEntity.builder().id(UUID.randomUUID()).documentId(UUID.randomUUID()).build();
        AddressUK address = AddressUK.builder()
            .addressLine1("1 High Street").postTown("London").postCode("W1 1AA").build();

        underTest.sendPack(pcsCase, recipient, LetterType.DEFENCE_PACK, 2, "Jane Doe", address, List.of(document));
        underTest.sendPack(pcsCase, recipient, LetterType.GEN_APP_PACK, 3, "Jane Doe", address, List.of(document));

        verify(sendLetterApi, times(2)).sendLetter(eq("s2s"), letterCaptor.capture());
        assertThat(letterCaptor.getAllValues()).extracting(letter -> letter.type)
            .containsExactly("DEF-02-IN0", "GEN-03-IN0");
    }

    @Test
    @DisplayName("Throws and sends nothing when the postal address is missing")
    void shouldThrowAndSendNothingWhenPostalAddressMissing() {
        AddressUK blank = AddressUK.builder().build();

        assertThatThrownBy(() -> underTest.sendPack(
            pcsCase, recipient, LetterType.DEFENDANT_CLAIM_PACK, "Jane Doe", blank, List.of()))
            .isInstanceOf(MissingPostalAddressException.class);

        verifyNoInteractions(coversheetProvider, coveringLetterProvider, letterDocumentFetcher, pdfMerger,
            sendLetterApi);
    }
}
