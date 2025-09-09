package uk.gov.hmcts.reform.pcs.ccd.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;
import uk.gov.hmcts.reform.pcs.testingsupport.model.DocAssemblyRequest;

@Slf4j
@Service
@AllArgsConstructor
public class DocumentGenerationService {

    private final DocAssemblyService docAssemblyService;

    public Document generateDocument(String templateId, Map<String, Object> formPayload, String outputType) {
        try {
            DocAssemblyRequest request = DocAssemblyRequest.builder()
                .templateId(templateId)
                .formPayload(formPayload)
                .outputType(outputType)
                .caseTypeId("CIVIL")
                .jurisdictionId("CIVIL")
                .secureDocStoreEnabled(true)
                .build();

            String documentUrl = docAssemblyService.generateDocument(request);

            String filename = generateFilename(templateId, outputType);

            return Document.builder()
                .filename(filename)
                .binaryUrl(documentUrl + "/binary")
                .url(documentUrl)
                .build();

        } catch (Exception e) {
            log.error("Failed to generate document with template: {}", templateId, e);
            throw new RuntimeException("Document generation failed", e);
        }
    }

    public Document generateDocument(String templateId, Map<String, Object> formPayload) {
        return generateDocument(templateId, formPayload, "PDF");
    }

    public Document generateDocumentFromCase(String templateId, Object caseData, String outputType) {
        Map<String, Object> formPayload = extractCaseDataForDocument(caseData);
        return generateDocument(templateId, formPayload, outputType);
    }

    public ListValue<Document> createDocumentListValue(Document document) {
        return ListValue.<Document>builder()
            .id(UUID.randomUUID().toString())
            .value(document)
            .build();
    }

    private String generateFilename(String templateId, String outputType) {
        String baseName = templateId.replaceAll("\\.docx?$", "");
        return String.format("%s_%s.%s", baseName, System.currentTimeMillis(), outputType.toLowerCase());
    }

    private Map<String, Object> extractCaseDataForDocument(Object caseData) {
        Map<String, Object> formPayload = new HashMap<>();

        // Add basic case information
        formPayload.put("generatedAt", System.currentTimeMillis());
        formPayload.put("documentType", "Generated Document");

        return formPayload;
    }
}
