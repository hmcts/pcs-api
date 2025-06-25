package uk.gov.hmcts.reform.pcs.document.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.testingsupport.model.DocAssemblyRequest;
import uk.gov.hmcts.reform.pcs.idam.IdamService;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

@Slf4j
@Service
public class DocAssemblyService {
    private final DocAssemblyApi docAssemblyApi;
    private final IdamService idamService;
    private final AuthTokenGenerator authTokenGenerator;
    private static final String DEFAULT_TEMPLATE_ID = "CV-SPC-CLM-ENG-01356.docx";

    public DocAssemblyService(
        DocAssemblyApi docAssemblyApi,
        IdamService idamService,
        AuthTokenGenerator authTokenGenerator
    ) {
        this.docAssemblyApi = docAssemblyApi;
        this.idamService = idamService;
        this.authTokenGenerator = authTokenGenerator;
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
        String authorization = idamService.getSystemUserAuthorisation();
        String serviceAuthorization = authTokenGenerator.generate();
        return docAssemblyApi.generateDocument(
            authorization,
            serviceAuthorization,
            request
        );
    }
} 