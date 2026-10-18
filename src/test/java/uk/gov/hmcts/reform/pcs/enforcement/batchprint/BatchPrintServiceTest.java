package uk.gov.hmcts.reform.pcs.enforcement.batchprint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.LetterDocumentFetcher;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.PdfMerger;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchPrintServiceTest {

    private static final String BAILIFF = "bob-id";
    private static final List<String> CHOSEN = List.of("1111111111111111", "2222222222222222", "3333333333333333");

    @Mock
    private BatchPrintPlanner batchPrintPlanner;
    @Mock
    private LetterDocumentFetcher letterDocumentFetcher;
    @Mock
    private PdfMerger pdfMerger;

    private BatchPrintService underTest;

    @BeforeEach
    void setUp() {
        underTest = new BatchPrintService(batchPrintPlanner, letterDocumentFetcher, pdfMerger);
    }

    @Test
    void shouldMergeEachCaseInTurnIntoOnePdf() {
        PrintableDocument warrantOne = document(BatchPrintDocumentKind.WARRANT);
        PrintableDocument noticeOne = document(BatchPrintDocumentKind.NOTICE_OF_EVICTION);
        PrintableDocument warrantTwo = document(BatchPrintDocumentKind.WARRANT);
        ExcludedCase notMine = ExcludedCase.of("3333333333333333", BatchPrintExclusion.NOT_ALLOCATED);
        when(batchPrintPlanner.plan(BAILIFF, CHOSEN)).thenReturn(new BatchPrintPlan(
            List.of(printable("1111111111111111", warrantOne, noticeOne), printable("2222222222222222", warrantTwo)),
            List.of(notMine)));
        byte[] warrantOneBytes = {1};
        byte[] noticeOneBytes = {2};
        byte[] warrantTwoBytes = {3};
        when(letterDocumentFetcher.fetchBytes(warrantOne.documentId())).thenReturn(warrantOneBytes);
        when(letterDocumentFetcher.fetchBytes(noticeOne.documentId())).thenReturn(noticeOneBytes);
        when(letterDocumentFetcher.fetchBytes(warrantTwo.documentId())).thenReturn(warrantTwoBytes);
        byte[] caseOne = {10};
        byte[] caseTwo = {20};
        byte[] combined = {30};
        when(pdfMerger.merge(List.of(warrantOneBytes, noticeOneBytes))).thenReturn(caseOne);
        when(pdfMerger.merge(List.of(warrantTwoBytes))).thenReturn(caseTwo);
        when(pdfMerger.merge(List.of(caseOne, caseTwo))).thenReturn(combined);

        BatchPrint batchPrint = underTest.print(BAILIFF, CHOSEN);

        assertThat(batchPrint.pdf()).isSameAs(combined);
        assertThat(batchPrint.plan().included()).extracting(PrintableCase::caseReference)
            .containsExactly("1111111111111111", "2222222222222222");
        assertThat(batchPrint.plan().excluded()).containsExactly(notMine);
    }

    @Test
    void shouldLeaveOutOnlyTheCaseWhoseDocumentCannotBeFetched() {
        PrintableDocument broken = document(BatchPrintDocumentKind.WARRANT);
        PrintableDocument fine = document(BatchPrintDocumentKind.WARRANT);
        when(batchPrintPlanner.plan(BAILIFF, CHOSEN)).thenReturn(new BatchPrintPlan(
            List.of(printable("1111111111111111", broken), printable("2222222222222222", fine)), List.of()));
        when(letterDocumentFetcher.fetchBytes(broken.documentId()))
            .thenThrow(new IllegalStateException("Document download returned no content"));
        byte[] fineBytes = {1};
        byte[] fineCase = {2};
        byte[] combined = {3};
        when(letterDocumentFetcher.fetchBytes(fine.documentId())).thenReturn(fineBytes);
        when(pdfMerger.merge(List.of(fineBytes))).thenReturn(fineCase);
        when(pdfMerger.merge(List.of(fineCase))).thenReturn(combined);

        BatchPrint batchPrint = underTest.print(BAILIFF, CHOSEN);

        assertThat(batchPrint.pdf()).isSameAs(combined);
        assertThat(batchPrint.plan().included()).extracting(PrintableCase::caseReference)
            .containsExactly("2222222222222222");
        assertThat(batchPrint.plan().excluded()).containsExactly(
            ExcludedCase.of("1111111111111111", BatchPrintExclusion.DOCUMENT_UNAVAILABLE));
    }

    @Test
    void shouldProduceNoFileWhenNoCaseCanBePrinted() {
        BatchPrintPlan nothing = new BatchPrintPlan(
            List.of(), List.of(ExcludedCase.of("1111111111111111", BatchPrintExclusion.NOT_ALLOCATED)));
        when(batchPrintPlanner.plan(BAILIFF, CHOSEN)).thenReturn(nothing);

        BatchPrint batchPrint = underTest.print(BAILIFF, CHOSEN);

        assertThat(batchPrint.isEmpty()).isTrue();
        assertThat(batchPrint.plan()).isEqualTo(nothing);
        verify(pdfMerger, never()).merge(anyList());
    }

    private static PrintableCase printable(String caseReference, PrintableDocument... documents) {
        return new PrintableCase(caseReference, "WP-1", List.of(documents), List.of());
    }

    private static PrintableDocument document(BatchPrintDocumentKind kind) {
        return new PrintableDocument(kind, kind + ".pdf", Instant.EPOCH, UUID.randomUUID());
    }
}
