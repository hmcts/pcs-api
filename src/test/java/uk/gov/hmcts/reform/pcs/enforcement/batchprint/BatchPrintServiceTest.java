package uk.gov.hmcts.reform.pcs.enforcement.batchprint;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.LetterDocumentFetcher;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    private RecordingMerger pdfMerger;
    private BatchPrintService underTest;

    @BeforeEach
    void setUp() {
        pdfMerger = new RecordingMerger();
        underTest = new BatchPrintService(batchPrintPlanner, letterDocumentFetcher, pdfMerger);
    }

    /**
     * The batch print file and the directory holding it outlive print(): in the service it is the request that
     * deletes them.
     */
    @AfterEach
    void deleteTempFiles() {
        pdfMerger.directories.forEach(BatchPrintTempDirectory::delete);
    }

    @Test
    void shouldMergeEachCaseInTurnIntoOnePdf() throws IOException {
        PrintableDocument warrantOne = document(BatchPrintDocumentKind.WARRANT);
        PrintableDocument noticeOne = document(BatchPrintDocumentKind.NOTICE_OF_EVICTION);
        PrintableDocument warrantTwo = document(BatchPrintDocumentKind.WARRANT);
        ExcludedCase notMine = ExcludedCase.of("3333333333333333", BatchPrintExclusion.NOT_ALLOCATED);
        when(batchPrintPlanner.plan(BAILIFF, CHOSEN)).thenReturn(new BatchPrintPlan(
            List.of(printable("1111111111111111", warrantOne, noticeOne), printable("2222222222222222", warrantTwo)),
            List.of(notMine)));
        when(letterDocumentFetcher.fetchBytes(warrantOne.documentId())).thenReturn(onePagePdf("Warrant 1"));
        when(letterDocumentFetcher.fetchBytes(noticeOne.documentId())).thenReturn(onePagePdf("Notice 1"));
        when(letterDocumentFetcher.fetchBytes(warrantTwo.documentId())).thenReturn(onePagePdf("Warrant 2"));

        BatchPrint batchPrint = underTest.print(BAILIFF, CHOSEN);

        assertThat(pageTexts(batchPrint)).containsExactly("Warrant 1", "Notice 1", "Warrant 2");
        assertThat(batchPrint.plan().included()).extracting(PrintableCase::caseReference)
            .containsExactly("1111111111111111", "2222222222222222");
        assertThat(batchPrint.plan().excluded()).containsExactly(notMine);
    }

    @Test
    void shouldLeaveOutOnlyTheCasesWhoseDocumentCannotBeFetchedOrRead() throws IOException {
        PrintableDocument missing = document(BatchPrintDocumentKind.WARRANT);
        PrintableDocument corrupt = document(BatchPrintDocumentKind.WARRANT);
        PrintableDocument fine = document(BatchPrintDocumentKind.WARRANT);
        when(batchPrintPlanner.plan(BAILIFF, CHOSEN)).thenReturn(new BatchPrintPlan(
            List.of(printable("1111111111111111", missing), printable("2222222222222222", fine),
                    printable("3333333333333333", corrupt)), List.of()));
        when(letterDocumentFetcher.fetchBytes(missing.documentId()))
            .thenThrow(new IllegalStateException("Document download returned no content"));
        when(letterDocumentFetcher.fetchBytes(corrupt.documentId())).thenReturn(new byte[] {1, 2, 3});
        when(letterDocumentFetcher.fetchBytes(fine.documentId())).thenReturn(onePagePdf("Fine"));

        BatchPrint batchPrint = underTest.print(BAILIFF, CHOSEN);

        assertThat(pageTexts(batchPrint)).containsExactly("Fine");
        assertThat(batchPrint.plan().included()).extracting(PrintableCase::caseReference)
            .containsExactly("2222222222222222");
        assertThat(batchPrint.plan().excluded()).containsExactly(
            ExcludedCase.of("1111111111111111", BatchPrintExclusion.DOCUMENT_UNAVAILABLE),
            ExcludedCase.of("3333333333333333", BatchPrintExclusion.DOCUMENT_UNAVAILABLE));
    }

    @Test
    void shouldProduceNoFileWhenNoCaseCanBePrinted() {
        BatchPrintPlan nothing = new BatchPrintPlan(
            List.of(), List.of(ExcludedCase.of("1111111111111111", BatchPrintExclusion.NOT_ALLOCATED)));
        when(batchPrintPlanner.plan(BAILIFF, CHOSEN)).thenReturn(nothing);

        BatchPrint batchPrint = underTest.print(BAILIFF, CHOSEN);

        assertThat(batchPrint.isEmpty()).isTrue();
        assertThat(batchPrint.plan()).isEqualTo(nothing);
        verify(letterDocumentFetcher, never()).fetchBytes(any());
        assertThat(pdfMerger.created).isEmpty();
        assertThat(batchPrint.directory()).isNull();
        assertThat(pdfMerger.directories).hasSize(1).noneMatch(Files::exists);
    }

    @Test
    void shouldFetchSeveralCasesAtOnceButNoMoreThanTheLimit() throws IOException {
        int limit = 3;
        underTest = new BatchPrintService(batchPrintPlanner, letterDocumentFetcher, pdfMerger, limit,
                                          BatchPrintService.MAX_WAIT_FOR_A_SLOT);
        List<PrintableCase> cases = IntStream.range(0, 12)
            .mapToObj(i -> printable(String.valueOf(1000000000000000L + i), document(BatchPrintDocumentKind.WARRANT)))
            .toList();
        List<String> references = cases.stream().map(PrintableCase::caseReference).toList();
        when(batchPrintPlanner.plan(BAILIFF, references)).thenReturn(new BatchPrintPlan(cases, List.of()));
        AtomicInteger inFlight = new AtomicInteger();
        AtomicInteger mostInFlight = new AtomicInteger();
        byte[] pdf = onePagePdf("Warrant");
        when(letterDocumentFetcher.fetchBytes(any())).thenAnswer(invocation -> {
            mostInFlight.accumulateAndGet(inFlight.incrementAndGet(), Math::max);
            Thread.sleep(50);
            inFlight.decrementAndGet();
            return pdf;
        });

        BatchPrint batchPrint = underTest.print(BAILIFF, references);

        assertThat(mostInFlight.get()).isBetween(2, limit);
        assertThat(pageTexts(batchPrint)).hasSize(12);
        assertThat(batchPrint.plan().included()).extracting(PrintableCase::caseReference)
            .containsExactlyElementsOf(references);
    }

    @Test
    void shouldLeaveOnlyTheBatchPrintFileBehind() throws IOException {
        PrintableDocument warrantOne = document(BatchPrintDocumentKind.WARRANT);
        PrintableDocument warrantTwo = document(BatchPrintDocumentKind.WARRANT);
        when(batchPrintPlanner.plan(BAILIFF, CHOSEN)).thenReturn(new BatchPrintPlan(
            List.of(printable("1111111111111111", warrantOne), printable("2222222222222222", warrantTwo)),
            List.of()));
        when(letterDocumentFetcher.fetchBytes(any())).thenReturn(onePagePdf("Warrant"));

        BatchPrint batchPrint = underTest.print(BAILIFF, CHOSEN);

        assertThat(pdfMerger.created).hasSize(3);
        assertThat(pdfMerger.created.subList(0, 2)).noneMatch(Files::exists);
        assertThat(batchPrint.pdf()).isEqualTo(pdfMerger.created.get(2)).exists();
        assertThat(Files.size(batchPrint.pdf())).isPositive();
        assertThat(batchPrint.directory()).isEqualTo(pdfMerger.directories.getFirst());
    }

    /**
     * The file holds several cases' warrants, risk assessments and notices of eviction, and the system temp
     * directory is writable by every user on the host, so a batch print is built in a directory of its own
     * that only the user pcs-api runs as can read.
     */
    @Test
    void shouldBuildTheBatchPrintInADirectoryOnlyItsOwnerCanRead() throws IOException {
        when(batchPrintPlanner.plan(BAILIFF, CHOSEN)).thenReturn(new BatchPrintPlan(
            List.of(printable("1111111111111111", document(BatchPrintDocumentKind.WARRANT))), List.of()));
        when(letterDocumentFetcher.fetchBytes(any())).thenReturn(onePagePdf("Warrant"));

        BatchPrint batchPrint = underTest.print(BAILIFF, CHOSEN);

        Path directory = batchPrint.directory();
        assertThat(batchPrint.pdf()).hasParent(directory);
        assertThat(directory).isNotEqualTo(Path.of(System.getProperty("java.io.tmpdir")));
        assertThat(Files.getPosixFilePermissions(directory))
            .isEqualTo(PosixFilePermissions.fromString("rwx------"));
        assertThat(Files.getPosixFilePermissions(batchPrint.pdf()))
            .isEqualTo(PosixFilePermissions.fromString("rw-------"));
    }

    /**
     * A case that cannot get a slot is not left out of the file: the bailiff is refused, because a batch print
     * that silently omits cases would be printed and acted on as if it were complete.
     */
    @Test
    void shouldRefuseTheWholeRequestAndLeaveNoFileWhenNoSlotComesFree() throws IOException {
        underTest = new BatchPrintService(batchPrintPlanner, letterDocumentFetcher, pdfMerger, 1,
                                          Duration.ofMillis(50));
        List<String> twoCases = CHOSEN.subList(0, 2);
        when(batchPrintPlanner.plan(BAILIFF, twoCases)).thenReturn(new BatchPrintPlan(
            List.of(printable(twoCases.get(0), document(BatchPrintDocumentKind.WARRANT)),
                    printable(twoCases.get(1), document(BatchPrintDocumentKind.WARRANT))),
            List.of()));
        byte[] pdf = onePagePdf("Warrant");
        when(letterDocumentFetcher.fetchBytes(any())).thenAnswer(invocation -> {
            Thread.sleep(500);
            return pdf;
        });

        assertThatThrownBy(() -> underTest.print(BAILIFF, twoCases))
            .isInstanceOf(BatchPrintBusyException.class)
            .hasMessage(BatchPrintBusyException.MESSAGE);

        assertThat(pdfMerger.created).hasSize(1).noneMatch(Files::exists);
        assertThat(pdfMerger.directories).hasSize(1).noneMatch(Files::exists);
    }

    /**
     * The slot budget is there to refuse a bailiff whose documents cannot be fetched because the instance is
     * busy with other bailiffs' batch prints. A full batch queues behind its own cases for several times the
     * budget, and must not refuse itself for doing so.
     */
    @Test
    void shouldPrintAFullBatchThatTakesLongerThanOneWavesWaitToFetch() throws IOException {
        Duration perWaveBudget = Duration.ofMillis(300);
        underTest = new BatchPrintService(batchPrintPlanner, letterDocumentFetcher, pdfMerger,
                                          BatchPrintService.MAX_CONCURRENT_CASES, perWaveBudget);
        List<PrintableCase> cases = IntStream.range(0, BatchPrintController.MAX_CASES)
            .mapToObj(i -> printable(String.valueOf(1000000000000000L + i), document(BatchPrintDocumentKind.WARRANT)))
            .toList();
        List<String> references = cases.stream().map(PrintableCase::caseReference).toList();
        when(batchPrintPlanner.plan(BAILIFF, references)).thenReturn(new BatchPrintPlan(cases, List.of()));
        byte[] pdf = onePagePdf("Warrant");
        when(letterDocumentFetcher.fetchBytes(any())).thenAnswer(invocation -> {
            Thread.sleep(perWaveBudget.dividedBy(2).toMillis());
            return pdf;
        });

        BatchPrint batchPrint = underTest.print(BAILIFF, references);

        assertThat(batchPrint.plan().included()).extracting(PrintableCase::caseReference)
            .containsExactlyElementsOf(references);
        assertThat(pageTexts(batchPrint)).hasSize(BatchPrintController.MAX_CASES);
    }

    /**
     * The refusal is still what a bailiff gets when the instance is genuinely busy: another request is holding
     * the slots for longer than this one will wait for them.
     */
    @Test
    void shouldRefuseTheRequestWhenAnotherRequestHoldsTheSlotsForLongerThanTheBudget() throws Exception {
        underTest = new BatchPrintService(batchPrintPlanner, letterDocumentFetcher, pdfMerger, 1,
                                          Duration.ofMillis(100));
        List<String> otherRequest = List.of("9999999999999999");
        List<String> thisRequest = CHOSEN.subList(0, 1);
        when(batchPrintPlanner.plan("other-bailiff", otherRequest)).thenReturn(new BatchPrintPlan(
            List.of(printable(otherRequest.getFirst(), document(BatchPrintDocumentKind.WARRANT))), List.of()));
        when(batchPrintPlanner.plan(BAILIFF, thisRequest)).thenReturn(new BatchPrintPlan(
            List.of(printable(thisRequest.getFirst(), document(BatchPrintDocumentKind.WARRANT))), List.of()));
        CountDownLatch slotTaken = new CountDownLatch(1);
        byte[] pdf = onePagePdf("Warrant");
        when(letterDocumentFetcher.fetchBytes(any())).thenAnswer(invocation -> {
            slotTaken.countDown();
            Thread.sleep(1000);
            return pdf;
        });

        Thread other = new Thread(() -> underTest.print("other-bailiff", otherRequest));
        other.start();
        assertThat(slotTaken.await(5, TimeUnit.SECONDS)).isTrue();

        assertThatThrownBy(() -> underTest.print(BAILIFF, thisRequest))
            .isInstanceOf(BatchPrintBusyException.class)
            .hasMessage(BatchPrintBusyException.MESSAGE);

        other.join();
    }

    private static List<String> pageTexts(BatchPrint batchPrint) throws IOException {
        byte[] bytes = Files.readAllBytes(batchPrint.pdf());
        try (PDDocument pdf = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            List<String> texts = new ArrayList<>();
            for (int page = 1; page <= pdf.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                texts.add(stripper.getText(pdf).trim());
            }
            return texts;
        }
    }

    private static byte[] onePagePdf(String text) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(72, 720);
                content.showText(text);
                content.endText();
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    private static PrintableCase printable(String caseReference, PrintableDocument... documents) {
        return new PrintableCase(caseReference, "WP-1", List.of(documents), List.of());
    }

    private static PrintableDocument document(BatchPrintDocumentKind kind) {
        return new PrintableDocument(kind, kind + ".pdf", Instant.EPOCH, UUID.randomUUID());
    }

    /**
     * The real merger, noting every directory it is asked for and every temp file it creates: case files
     * first, in completion order, then the batch print file.
     */
    private static final class RecordingMerger extends BatchPrintPdfMerger {

        private final List<Path> created = Collections.synchronizedList(new ArrayList<>());
        private final List<Path> directories = Collections.synchronizedList(new ArrayList<>());

        @Override
        public Path createMergeDirectory() throws IOException {
            Path directory = super.createMergeDirectory();
            directories.add(directory);
            return directory;
        }

        @Override
        public Path mergeCase(Path directory, List<byte[]> documents) throws IOException {
            Path file = super.mergeCase(directory, documents);
            created.add(file);
            return file;
        }

        @Override
        public Path mergeCases(Path directory, List<Path> caseFiles) throws IOException {
            Path file = super.mergeCases(directory, caseFiles);
            created.add(file);
            return file;
        }
    }
}
