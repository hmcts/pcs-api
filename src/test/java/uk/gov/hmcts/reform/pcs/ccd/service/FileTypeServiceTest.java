package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

public class FileTypeServiceTest {

    private FileTypeService fileTypeService;

    @BeforeEach
    void setUp() {
        fileTypeService = new FileTypeService();
    }

    @ParameterizedTest
    @MethodSource("multimediaFiles")
    void shouldReturnErrorWhenMultiMediaFileIsUploaded(String fileName) {
        // Given
        Document document = Document.builder().filename(fileName).build();
        ListValue<Document> documentListValue = new ListValue<>();
        documentListValue.setValue(document);
        List<String> errors = new ArrayList<>();

        // When
        fileTypeService.validateNonMultiMediaFiles(List.of(documentListValue), errors);

        // Then
        assertThat(errors).hasSize(1);
        assertThat(errors.getFirst()).isEqualTo(fileName + " contains a disallowed file type");
    }

    @ParameterizedTest
    @MethodSource("nonMultimediaFiles")
    void shouldNotReturnErrorWhenNonMultiMediaFileIsUploaded(String fileName) {
        // Given
        Document document = Document.builder().filename(fileName).build();
        ListValue<Document> documentListValue = new ListValue<>();
        documentListValue.setValue(document);
        List<String> errors = new ArrayList<>();

        // When
        fileTypeService.validateNonMultiMediaFiles(List.of(documentListValue), errors);

        // Then
        assertThat(errors).hasSize(0);
    }

    private static Stream<Arguments> multimediaFiles() {
        return Stream.of(
            argumentSet("mp3", "test.mp3"),
            argumentSet("mp4", "test.mp4"),
            argumentSet("mp4a", "test.m4a"),
            argumentSet("mpeg", "test.mpeg")
        );
    }

    private static Stream<Arguments> nonMultimediaFiles() {
        return Stream.of(
            argumentSet("jpeg", "test.jpeg"),
            argumentSet("doc", "test.doc"),
            argumentSet("docx", "test.dox")
        );
    }
}
