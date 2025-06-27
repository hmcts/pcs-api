package uk.gov.hmcts.reform.pcs.document.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
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
        
        try {
            String response = docAssemblyApi.generateDocument(
                authorization,
                serviceAuthorization,
                request
            );
            
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
        } catch (FeignException e) {
            handleFeignException(e);
            throw new DocAssemblyException("Unexpected error occurred during document generation", e);
        } catch (DocAssemblyException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse Doc Assembly response", e);
            throw new DocAssemblyException("Failed to parse Doc Assembly response", e);
        }
    }
    
    private void handleFeignException(FeignException e) {
        int status = e.status();
        String message = e.getMessage();
        
        log.error("Doc Assembly API call failed with status {}: {}", status, message, e);
        
        switch (status) {
            case 400:
                throw new DocAssemblyException("Bad request to Doc Assembly service: " + message, e);
            case 401:
            case 403:
                throw new DocAssemblyException("Authorization failed for Doc Assembly service: " + message, e);
            case 404:
                throw new DocAssemblyException("Doc Assembly service endpoint not found: " + message, e);
            case 500:
            case 502:
            case 503:
            case 504:
                throw new DocAssemblyException("Doc Assembly service is temporarily unavailable: " + message, e);
            default:
                if (status >= 500) {
                    throw new DocAssemblyException("Doc Assembly service error: " + message, e);
                } else {
                    throw new DocAssemblyException("Doc Assembly service request failed: " + message, e);
                }
        }
    }
} 