package uk.gov.hmcts.reform.pcs.document.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.testingsupport.model.DocAssemblyRequest;

@Slf4j
@Service
public class DocAssemblyService {
    private final DocAssemblyApi docAssemblyApi;
    private static final String DEFAULT_TEMPLATE_ID = "CV-SPC-CLM-ENG-01356.docx";

    public DocAssemblyService(
        DocAssemblyApi docAssemblyApi
    ) {
        this.docAssemblyApi = docAssemblyApi;
    }

    public String generateDocument(DocAssemblyRequest request, String authorization, String serviceAuthorization) {
        // Use templateId from request, or default if not provided
        if (request.getTemplateId() == null || request.getTemplateId().trim().isEmpty()) {
            request.setTemplateId(DEFAULT_TEMPLATE_ID);
        }
        // Set output type if not provided
        if (request.getOutputType() == null || request.getOutputType().trim().isEmpty()) {
            request.setOutputType("PDF");
        }
        return docAssemblyApi.generateDocument(
            authorization,
            serviceAuthorization,
            request
        );
    }
} 