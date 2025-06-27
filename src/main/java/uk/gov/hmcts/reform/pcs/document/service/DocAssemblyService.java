package uk.gov.hmcts.reform.pcs.document.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.testingsupport.model.DocAssemblyRequest;
import uk.gov.hmcts.reform.pcs.idam.IdamService;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import java.util.Base64;
import uk.gov.hmcts.reform.pcs.document.service.exception.DocAssemblyException;

@Slf4j
@Service
public class DocAssemblyService {
    private final DocAssemblyApi docAssemblyApi;
    private final IdamService idamService;
    private final AuthTokenGenerator authTokenGenerator;
    private final ObjectMapper objectMapper;
    private static final String DEFAULT_TEMPLATE_ID = "CV-SPC-CLM-ENG-01356.docx";

    public DocAssemblyService(
        DocAssemblyApi docAssemblyApi,
        IdamService idamService,
        AuthTokenGenerator authTokenGenerator,
        ObjectMapper objectMapper
    ) {
        this.docAssemblyApi = docAssemblyApi;
        this.idamService = idamService;
        this.authTokenGenerator = authTokenGenerator;
        this.objectMapper = objectMapper;
    }

    public String generateDocument(DocAssemblyRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        // Use templateId from request, or default if not provided
        if (request.getTemplateId() == null || request.getTemplateId().trim().isEmpty()) {
            request.setTemplateId(DEFAULT_TEMPLATE_ID);
        }
        // Set output type if not provided
        if (request.getOutputType() == null || request.getOutputType().trim().isEmpty()) {
            request.setOutputType("PDF");
        }
        
        // Encode template ID to base64 as required by Doc Assembly service
        String encodedTemplateId = Base64.getEncoder().encodeToString(request.getTemplateId().getBytes());
        request.setTemplateId(encodedTemplateId);
        
        String authorization = idamService.getSystemUserAuthorisation();
        String serviceAuthorization = authTokenGenerator.generate();
        
        String response = docAssemblyApi.generateDocument(
            authorization,
            serviceAuthorization,
            request
        );
        
        try {
            // Parse the JSON response to extract the document URL
            JsonNode jsonNode = objectMapper.readTree(response);
            JsonNode renditionOutputLocationNode = jsonNode.get("renditionOutputLocation");
            String documentUrl = null;
            if (renditionOutputLocationNode != null && !renditionOutputLocationNode.isNull()) {
                documentUrl = renditionOutputLocationNode.asText();
            }
            if (documentUrl == null || documentUrl.isEmpty()) {
                log.error("No or empty renditionOutputLocation found in Doc Assembly response: {}", response);
                throw new DocAssemblyException("No document URL returned from Doc Assembly service");
            }
            log.info("Document generated successfully. URL: {}", documentUrl);
            return documentUrl;
        } catch (DocAssemblyException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse Doc Assembly response: {}", response, e);
            throw new DocAssemblyException("Failed to parse Doc Assembly response", e);
        }
    }
} 