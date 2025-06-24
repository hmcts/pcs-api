package uk.gov.hmcts.reform.pcs.document.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.pcs.testingsupport.model.DocAssemblyRequest;

@Slf4j
@Service
public class DocAssemblyService {
    private final RestTemplate restTemplate;
    private final String docAssemblyUrl;
    private static final String DEFAULT_TEMPLATE_ID = "CV-SPC-CLM-ENG-01356.docx";

    public DocAssemblyService(
        RestTemplate restTemplate,
        @Value("${doc-assembly.url}") String docAssemblyUrl
    ) {
        this.restTemplate = restTemplate;
        this.docAssemblyUrl = docAssemblyUrl;
    }

    public String generateDocument(DocAssemblyRequest request, String authorization, String serviceAuthorization) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", authorization);
        headers.set("ServiceAuthorization", serviceAuthorization);

        // Use templateId from request, or default if not provided
        if (request.getTemplateId() == null || request.getTemplateId().trim().isEmpty()) {
            request.setTemplateId(DEFAULT_TEMPLATE_ID);
        }
        
        // Set output type if not provided
        if (request.getOutputType() == null || request.getOutputType().trim().isEmpty()) {
            request.setOutputType("PDF");
        }

        HttpEntity<DocAssemblyRequest> requestEntity = new HttpEntity<>(request, headers);
        
        return restTemplate.postForObject(
            docAssemblyUrl + "/api/template-renditions",
            requestEntity,
            String.class
        );
    }
} 