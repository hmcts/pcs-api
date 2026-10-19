package uk.gov.hmcts.reform.pcs.enforcement.testcasesupport;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;
import uk.gov.hmcts.reform.ccd.document.am.model.CaseDocumentsMetadata;
import uk.gov.hmcts.reform.ccd.document.am.model.Classification;
import uk.gov.hmcts.reform.ccd.document.am.model.Document;
import uk.gov.hmcts.reform.ccd.document.am.model.DocumentHashToken;
import uk.gov.hmcts.reform.ccd.document.am.model.DocumentUploadRequest;
import uk.gov.hmcts.reform.pcs.ccd.CaseType;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentIdExtractor;
import uk.gov.hmcts.reform.pcs.enforcement.batchprint.BatchPrintDocumentKind;
import uk.gov.hmcts.reform.pcs.enforcement.document.EnforcementOutput;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.service.CurrentEnforcementService;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementDocumentService;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Files a one-page placeholder PDF in a case's Enforcement folder, against its current enforcement, for the
 * end to end tests (batch print, HDPI-8012). pcs-api generates the warrant, risk assessment and notice of
 * eviction itself ({@link uk.gov.hmcts.reform.pcs.enforcement.document.EnforcementDocumentGenerator}); this
 * files a later copy of one, which batch print then takes as the latest, for set-ups the suite cannot drive
 * yet, such as a rescheduled notice of eviction.
 *
 * <p>The PDF is uploaded through CDAM and attached to the case, as generated documents are, so it can be
 * fetched like a real one. CDAM needs dm-store behind it, which a CFTLib stack only runs with
 * {@code LOCAL_DOC_SERVICES=true}.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "testing-support.enabled", havingValue = "true")
public class EnforcementTestDocumentSeeder {

    private static final String PDF = "application/pdf";

    private final PcsCaseService pcsCaseService;
    private final CurrentEnforcementService currentEnforcementService;
    private final CaseDocumentClientApi caseDocumentClientApi;
    private final AuthTokenGenerator authTokenGenerator;
    private final IdamTokenProvider systemUpdateUserTokenProvider;
    private final DocumentIdExtractor documentIdExtractor;

    public EnforcementTestDocumentSeeder(PcsCaseService pcsCaseService,
                                         CurrentEnforcementService currentEnforcementService,
                                         CaseDocumentClientApi caseDocumentClientApi,
                                         AuthTokenGenerator authTokenGenerator,
                                         @Qualifier("systemUpdateUserTokenProvider")
                                         IdamTokenProvider systemUpdateUserTokenProvider,
                                         DocumentIdExtractor documentIdExtractor) {
        this.pcsCaseService = pcsCaseService;
        this.currentEnforcementService = currentEnforcementService;
        this.caseDocumentClientApi = caseDocumentClientApi;
        this.authTokenGenerator = authTokenGenerator;
        this.systemUpdateUserTokenProvider = systemUpdateUserTokenProvider;
        this.documentIdExtractor = documentIdExtractor;
    }

    /**
     * Files the placeholder, titled {@code title}, and returns its file name.
     *
     * @return empty when the case has no enforcement to file it against
     */
    @Transactional
    public Optional<String> seed(long caseReference, BatchPrintDocumentKind kind, String title) {
        PcsCaseEntity pcsCase = pcsCaseService.loadCase(caseReference);
        Optional<EnforcementOrderEntity> enforcement = currentEnforcementService.findCurrentEnforcement(pcsCase);
        if (enforcement.isEmpty()) {
            return Optional.empty();
        }

        String fileName = title.replaceAll("[^A-Za-z0-9 -]", "").trim() + ".pdf";
        byte[] pdf = render(title, caseReference);
        Document uploaded = upload(fileName, pdf);
        attach(caseReference, uploaded);

        DocumentEntity document = DocumentEntity.builder()
            .url(uploaded.links.self.href)
            .binaryUrl(uploaded.links.binary.href)
            .documentId(documentIdExtractor.extractDocumentId(uploaded.links.self.href))
            .fileName(fileName)
            .contentType(PDF)
            .size((long) pdf.length)
            .categoryId(EnforcementDocumentService.ENFORCEMENT_CATEGORY_ID)
            .type(documentType(kind, enforcement.get()))
            .build();
        pcsCase.addDocument(document);
        enforcement.get().addDocuments(List.of(document));
        log.info("Testing support: filed placeholder {} '{}' on case {}", kind, title, caseReference);
        return Optional.of(fileName);
    }

    /**
     * The type the enforcement document generator files this kind of document as on this enforcement, so the
     * placeholder is found, and shown or hidden, exactly as a generated one would be.
     */
    static DocumentType documentType(BatchPrintDocumentKind kind, EnforcementOrderEntity enforcement) {
        return switch (kind) {
            case WARRANT -> Optional.ofNullable(SelectEnforcementType.from(enforcement.getEnforcementOrder()))
                .map(type -> EnforcementOutput.forApproval(type).getDocumentType())
                .filter(kind.getDocumentTypes()::contains)
                .orElse(DocumentType.WARRANT_OF_POSSESSION);
            case RISK_ASSESSMENT -> EnforcementOutput.RISK_ASSESSMENT.getDocumentType();
            case NOTICE_OF_EVICTION -> EnforcementOutput.NOTICE_OF_EVICTION.getDocumentType();
        };
    }

    private Document upload(String fileName, byte[] pdf) {
        DocumentUploadRequest request = new DocumentUploadRequest(
            Classification.RESTRICTED.toString(), CaseType.getCaseType(), CaseType.getJurisdictionId(),
            List.of(new InMemoryPdf(fileName, pdf)));
        return caseDocumentClientApi.uploadDocuments(
            systemUpdateUserTokenProvider.getAuthToken(), authTokenGenerator.generate(), request)
            .getDocuments().getFirst();
    }

    private void attach(long caseReference, Document uploaded) {
        caseDocumentClientApi.patchDocument(
            systemUpdateUserTokenProvider.getAuthToken(),
            authTokenGenerator.generate(),
            CaseDocumentsMetadata.builder()
                .caseId(String.valueOf(caseReference))
                .caseTypeId(CaseType.getCaseType())
                .jurisdictionId(CaseType.getJurisdictionId())
                .documentHashTokens(List.of(DocumentHashToken.builder()
                                                .id(documentIdExtractor.extractDocumentId(uploaded.links.self.href)
                                                        .toString())
                                                .hashToken(uploaded.hashToken)
                                                .build()))
                .build());
    }

    static byte[] render(String title, long caseReference) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 14);
                content.newLineAtOffset(72, 720);
                content.showText(title);
                content.newLineAtOffset(0, -24);
                content.showText("Case " + caseReference);
                content.newLineAtOffset(0, -24);
                content.showText("Placeholder document for end to end tests");
                content.endText();
            }
            document.save(output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to render the placeholder PDF", e);
        }
    }

    /**
     * {@link MultipartFile} hands its content out as a byte array, so the content is held as one; equality is
     * on the content itself rather than on the array's identity.
     */
    private record InMemoryPdf(String fileName, byte[] content) implements MultipartFile {

        @Override
        public String getName() {
            return "files";
        }

        @Override
        public String getOriginalFilename() {
            return fileName;
        }

        @Override
        public String getContentType() {
            return PDF;
        }

        @Override
        public boolean isEmpty() {
            return content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() {
            return content.clone();
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(File destination) throws IOException {
            Files.write(destination.toPath(), content);
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof InMemoryPdf pdf)) {
                return false;
            }
            return Objects.equals(fileName, pdf.fileName) && Arrays.equals(content, pdf.content);
        }

        @Override
        public int hashCode() {
            return Objects.hash(fileName, Arrays.hashCode(content));
        }

        @Override
        public String toString() {
            return "InMemoryPdf[fileName=%s, content=%d bytes]".formatted(fileName, content.length);
        }
    }
}
