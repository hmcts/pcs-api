package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocument;
import uk.gov.hmcts.reform.pcs.ccd.service.FileUploadValidationService.ConditionalDocumentUpload;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.service.FileUploadValidationService.ALLOWED_FILE_TYPE_GUIDANCE;
import static uk.gov.hmcts.reform.pcs.ccd.service.FileUploadValidationService.DISALLOWED_FILE_TYPE_ERROR;
import static uk.gov.hmcts.reform.pcs.ccd.testutil.DocumentTestData.additionalDocumentsWithFilenames;
import static uk.gov.hmcts.reform.pcs.ccd.testutil.DocumentTestData.documentsWithFilenames;

@DisplayName("FileUploadValidationService Tests")
class FileUploadValidationServiceTest {

    private FileUploadValidationService fileUploadValidationService;

    @BeforeEach
    void setUp() {
        fileUploadValidationService = new FileUploadValidationService();
    }

    private static Stream<String> allowedExtensions() {
        return FileUploadValidationService.ALLOWED_FILE_EXTENSIONS.stream();
    }

    @Nested
    @DisplayName("validateDocuments Method Tests")
    class ValidateDocumentsTests {

        @ParameterizedTest
        @ValueSource(strings = {
            "recording.mp3", "audio.m4a", "video.mp4", "clip.mpeg", "clip.mpg"
        })
        @DisplayName("Should return error when a document has a blocked multimedia extension")
        void shouldReturnErrorForBlockedExtension(String filename) {
            List<String> errors = fileUploadValidationService.validateDocuments(documentsWithFilenames(filename));

            assertThat(errors).containsExactly(DISALLOWED_FILE_TYPE_ERROR, ALLOWED_FILE_TYPE_GUIDANCE);
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "RECORDING.MP3", "Video.Mp4", "clip.MPEG"
        })
        @DisplayName("Should treat blocked extensions case-insensitively")
        void shouldTreatBlockedExtensionsCaseInsensitively(String filename) {
            List<String> errors = fileUploadValidationService.validateDocuments(documentsWithFilenames(filename));

            assertThat(errors).containsExactly(DISALLOWED_FILE_TYPE_ERROR, ALLOWED_FILE_TYPE_GUIDANCE);
        }

        @ParameterizedTest
        @MethodSource("uk.gov.hmcts.reform.pcs.ccd.service.FileUploadValidationServiceTest#allowedExtensions")
        @DisplayName("Should return no error for every allowed file extension")
        void shouldReturnNoErrorForAllowedExtensions(String extension) {
            List<String> errors = fileUploadValidationService.validateDocuments(
                documentsWithFilenames("document." + extension));

            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("Should return the error only once when multiple documents are blocked")
        void shouldReturnErrorOnceForMultipleBlockedDocuments() {
            List<String> errors = fileUploadValidationService.validateDocuments(
                documentsWithFilenames("one.mp3", "two.mp4", "valid.pdf"));

            assertThat(errors).containsExactly(DISALLOWED_FILE_TYPE_ERROR, ALLOWED_FILE_TYPE_GUIDANCE);
        }

        @Test
        @DisplayName("Should return no error when the list is null")
        void shouldReturnNoErrorWhenNull() {
            assertThat(fileUploadValidationService.validateDocuments(null)).isEmpty();
        }

        @Test
        @DisplayName("Should return no error when the list is empty")
        void shouldReturnNoErrorWhenEmpty() {
            assertThat(fileUploadValidationService.validateDocuments(List.of())).isEmpty();
        }

        @Test
        @DisplayName("Should ignore null document values within the list")
        void shouldIgnoreNullDocumentValues() {
            List<ListValue<Document>> documents = new ArrayList<>();
            documents.add(ListValue.<Document>builder().value(null).build());

            assertThat(fileUploadValidationService.validateDocuments(documents)).isEmpty();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Should return error when a document filename is null or blank")
        void shouldReturnErrorForNullOrBlankFilename(String filename) {
            List<String> errors = fileUploadValidationService.validateDocuments(documentsWithFilenames(filename));

            assertThat(errors).containsExactly(DISALLOWED_FILE_TYPE_ERROR, ALLOWED_FILE_TYPE_GUIDANCE);
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "noextension", "trailingdot.", "archive.mp3.zip"
        })
        @DisplayName("Should return error for a missing or non-allowlisted final extension")
        void shouldReturnErrorForUnrecognisedOrMissingExtension(String filename) {
            List<String> errors = fileUploadValidationService.validateDocuments(documentsWithFilenames(filename));

            assertThat(errors).containsExactly(DISALLOWED_FILE_TYPE_ERROR, ALLOWED_FILE_TYPE_GUIDANCE);
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "archive.zip", "installer.exe", "clip.mov", "page.html"
        })
        @DisplayName("Should return error for any file type outside the allowlist")
        void shouldReturnErrorForNonAllowlistedTypes(String filename) {
            List<String> errors = fileUploadValidationService.validateDocuments(documentsWithFilenames(filename));

            assertThat(errors).containsExactly(DISALLOWED_FILE_TYPE_ERROR, ALLOWED_FILE_TYPE_GUIDANCE);
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "REPORT.PDF", "Photo.JPG", "Notice.DOCX", "scan.TIFF"
        })
        @DisplayName("Should treat allowed extensions case-insensitively")
        void shouldAllowAllowedTypesCaseInsensitively(String filename) {
            List<String> errors = fileUploadValidationService.validateDocuments(documentsWithFilenames(filename));

            assertThat(errors).isEmpty();
        }
    }

    @Nested
    @DisplayName("validateConditionalDocuments Method Tests")
    class ValidateConditionalDocumentsTests {

        @Test
        @DisplayName("Should return the disallowed error once when any upload contains a blocked file")
        void shouldReturnErrorOnceWhenAnyUploadBlocked() {
            List<String> errors = fileUploadValidationService.validateConditionalDocuments(List.of(
                new ConditionalDocumentUpload(true, documentsWithFilenames("epc.pdf"), "missing epc"),
                new ConditionalDocumentUpload(true, documentsWithFilenames("gas.mp3"), "missing gas"),
                new ConditionalDocumentUpload(true, documentsWithFilenames("eicr.mp4"), "missing eicr")));

            assertThat(errors).containsExactly(DISALLOWED_FILE_TYPE_ERROR, ALLOWED_FILE_TYPE_GUIDANCE);
        }

        @Test
        @DisplayName("Should return no error when every upload is present and of an allowed type")
        void shouldReturnNoErrorWhenAllUploadsAllowed() {
            List<String> errors = fileUploadValidationService.validateConditionalDocuments(List.of(
                new ConditionalDocumentUpload(true, documentsWithFilenames("epc.pdf"), "missing epc"),
                new ConditionalDocumentUpload(true, documentsWithFilenames("gas.pdf"), "missing gas"),
                new ConditionalDocumentUpload(true, documentsWithFilenames("eicr.pdf"), "missing eicr")));

            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("Should return the missing message only for uploads that are required but empty")
        void shouldReturnMissingMessageForRequiredEmptyUploads() {
            List<String> errors = fileUploadValidationService.validateConditionalDocuments(List.of(
                new ConditionalDocumentUpload(true, null, "missing epc"),
                new ConditionalDocumentUpload(false, null, "missing gas"),
                new ConditionalDocumentUpload(true, documentsWithFilenames("eicr.pdf"), "missing eicr")));

            assertThat(errors).containsExactly("missing epc");
        }

        @Test
        @DisplayName("Should list every missing message then the disallowed error at most once")
        void shouldReturnMissingMessagesThenDisallowedErrorOnce() {
            List<String> errors = fileUploadValidationService.validateConditionalDocuments(List.of(
                new ConditionalDocumentUpload(true, null, "missing epc"),
                new ConditionalDocumentUpload(true, documentsWithFilenames("gas.mp3"), "missing gas"),
                new ConditionalDocumentUpload(true, documentsWithFilenames("eicr.mp4"), "missing eicr")));

            assertThat(errors)
                .containsExactly("missing epc", DISALLOWED_FILE_TYPE_ERROR, ALLOWED_FILE_TYPE_GUIDANCE);
        }
    }

    @Nested
    @DisplayName("validateAdditionalDocuments Method Tests")
    class ValidateAdditionalDocumentsTests {

        @Test
        @DisplayName("Should return error when an additional document has a blocked extension")
        void shouldReturnErrorForBlockedAdditionalDocument() {
            List<String> errors = fileUploadValidationService.validateAdditionalDocuments(
                additionalDocumentsWithFilenames("clip.mp4"));

            assertThat(errors).containsExactly(DISALLOWED_FILE_TYPE_ERROR, ALLOWED_FILE_TYPE_GUIDANCE);
        }

        @Test
        @DisplayName("Should return no error for an allowed additional document")
        void shouldReturnNoErrorForAllowedAdditionalDocument() {
            List<String> errors = fileUploadValidationService.validateAdditionalDocuments(
                additionalDocumentsWithFilenames("statement.pdf"));

            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("Should return no error when the additional documents list is null")
        void shouldReturnNoErrorWhenNull() {
            assertThat(fileUploadValidationService.validateAdditionalDocuments(null)).isEmpty();
        }

        @Test
        @DisplayName("Should ignore an additional document with a null inner document")
        void shouldIgnoreNullInnerDocument() {
            AdditionalDocument additionalDocument = AdditionalDocument.builder().document(null).build();
            List<ListValue<AdditionalDocument>> documents =
                List.of(ListValue.<AdditionalDocument>builder().value(additionalDocument).build());

            assertThat(fileUploadValidationService.validateAdditionalDocuments(documents)).isEmpty();
        }
    }

    @Nested
    @DisplayName("validateRequiredAdditionalDocuments Method Tests")
    class ValidateRequiredAdditionalDocumentsTests {

        private static final String REQUIRED_MESSAGE = "You must upload a document";

        @Test
        @DisplayName("Should return the required message when no additional document has been uploaded")
        void shouldReturnRequiredMessageWhenEmpty() {
            assertThat(fileUploadValidationService.validateRequiredAdditionalDocuments(null, REQUIRED_MESSAGE))
                .containsExactly(REQUIRED_MESSAGE);
        }

        @Test
        @DisplayName("Should return the disallowed error when a blocked additional file is uploaded")
        void shouldReturnDisallowedErrorWhenBlockedFileUploaded() {
            assertThat(fileUploadValidationService.validateRequiredAdditionalDocuments(
                additionalDocumentsWithFilenames("clip.mp4"), REQUIRED_MESSAGE))
                .containsExactly(DISALLOWED_FILE_TYPE_ERROR, ALLOWED_FILE_TYPE_GUIDANCE);
        }

        @Test
        @DisplayName("Should return no error when an allowed additional file is uploaded")
        void shouldReturnNoErrorWhenAllowedFileUploaded() {
            assertThat(fileUploadValidationService.validateRequiredAdditionalDocuments(
                additionalDocumentsWithFilenames("statement.pdf"), REQUIRED_MESSAGE))
                .isEmpty();
        }
    }

    @Nested
    @DisplayName("Guidance and allowlist consistency")
    class GuidanceConsistencyTests {

        @Test
        @DisplayName("Every allowed extension is mentioned in the user-facing guidance")
        void everyAllowedExtensionAppearsInGuidance() {
            String guidance = ALLOWED_FILE_TYPE_GUIDANCE.toLowerCase(Locale.UK);

            assertThat(FileUploadValidationService.ALLOWED_FILE_EXTENSIONS)
                .allSatisfy(extension -> assertThat(guidance).contains(extension));
        }

        @Test
        @DisplayName("Every extension listed in the guidance is in the allowlist")
        void everyGuidanceExtensionIsAllowed() {
            String extensionsPart = ALLOWED_FILE_TYPE_GUIDANCE
                .replaceFirst("(?i)^the selected file must be a\\s*", "")
                .toLowerCase(Locale.UK);

            for (String token : extensionsPart.split("[,\\s/.]+")) {
                if (!token.isEmpty()) {
                    assertThat(FileUploadValidationService.ALLOWED_FILE_EXTENSIONS).contains(token);
                }
            }
        }
    }
}
